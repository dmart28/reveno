/** 
 *  Copyright (c) 2015 The original author or authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.reveno.atp.core.engine;

import org.reveno.atp.api.commands.EmptyResult;
import org.reveno.atp.api.commands.Result;
import org.reveno.atp.api.exceptions.ReplicationFailedException;
import org.reveno.atp.api.transaction.TransactionInterceptor;
import org.reveno.atp.api.transaction.TransactionStage;
import org.reveno.atp.commons.BoolBiConsumer;
import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.api.channel.Channel;
import org.reveno.atp.core.channel.ChannelBuffer;
import org.reveno.atp.core.disruptor.ProcessorContext;
import org.reveno.atp.core.engine.components.TransactionExecutor;
import org.reveno.atp.utils.MeasureUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public class InputHandlers {

	public static final EmptyResult EMPTY_RESULT = new EmptyResult();

	@SuppressWarnings("unchecked")
	public void ex(ProcessorContext c, boolean filter, boolean eob, BoolBiConsumer<ProcessorContext> body) {
		if (!c.isAborted() && filter) {
			try {
				body.accept(c, eob);
			} catch (Throwable t) {
				log.error("inputHandlers", t);
				c.abort(t);
				if (c.future() != null) {
					c.future().complete(new EmptyResult(t));
				}
			}
		}
	}
	
	public void replication(ProcessorContext c, boolean endOfBatch) {
		ex(c, !c.isRestore() && !c.isReplicated(), endOfBatch, replicator);
	}
	
	public void transactionExecution(ProcessorContext c, boolean endOfBatch) {
		ex(c, true, endOfBatch, transactionExecutor);
	}
	
	public void journaling(ProcessorContext c, boolean endOfBatch) {
		ex(c, c.getTransactions().size() > 0 && !c.isRestore(), endOfBatch, journaler);
	}
	
	public void viewsUpdate(ProcessorContext c, boolean endOfBatch) {
		ex(c, true, endOfBatch, viewsUpdater);
	}
	
	public void eventsPublishing(ProcessorContext c, boolean endOfBatch) {
		ex(c, c.getEvents().size() > 0, endOfBatch, eventsPublisher);
	}
	
	@SuppressWarnings("unchecked")
	public void result(ProcessorContext c, boolean endOfBatch) {
		if (!c.isRestore()) {
			if (c.isAborted())
				c.future().complete(new EmptyResult(c.abortIssue()));
			else {
				if (c.hasResult())
					c.future().complete(new Result<>(c.commandResult()));
				else
					c.future().complete(EMPTY_RESULT);
			}
		}
	}
	
	public void destroy() {
	}

	protected WorkflowContext services;
	protected TransactionExecutor txExecutor;
	protected LongSupplier nextTransactionId;
	protected ProcessorContext ctxJ;
	protected final Consumer<Buffer> journalerConsumer = b -> {
		ctxJ.commitInfo().transactionId(ctxJ.transactionId()).time(ctxJ.time()).transactionCommits(ctxJ.getTransactions());
		services.serializer().serialize(ctxJ.commitInfo(), b);
	};

	private boolean changedClassLoaderReplicator = false;
	protected final BoolBiConsumer<ProcessorContext> replicator = (c, eob) -> {
		interceptors(TransactionStage.REPLICATION, c);
		if (!changedClassLoaderReplicator) {
			changeClassLoaderIfRequired();
			changedClassLoaderReplicator = true;
		}
		
		if (!services.failoverManager().replicate(b -> services.serializer().serializeCommands(c.getCommands(), b)))
			c.abort(new ReplicationFailedException());
	};
	protected final BoolBiConsumer<ProcessorContext> transactionExecutor = (c, eob) -> {
		if (!c.isRestore())
			c.transactionId(nextTransactionId.getAsLong());
		
		interceptors(TransactionStage.TRANSACTION, c);
		txExecutor.executeCommands(c, services);

	};
	private boolean changedClassLoaderJournaler = false;
	protected final BoolBiConsumer<ProcessorContext> journaler = (c, eob) -> {
		interceptors(TransactionStage.JOURNALING, c);

		rollIfRequired(c.transactionId());
		this.ctxJ = c;
		if (!changedClassLoaderJournaler) {
			changeClassLoaderIfRequired();
			changedClassLoaderJournaler = true;
		}
		services.transactionJournaler().writeData(journalerConsumer, eob);
	};
	protected final BoolBiConsumer<ProcessorContext> viewsUpdater = (c, eob) -> {
		services.viewsProcessor().process(c.getMarkedRecords());
	};
	protected final BoolBiConsumer<ProcessorContext> eventsPublisher = (c, eob) -> {
		if (c.isReplicated()) {
			services.eventPublisher().replicateEvents(c.transactionId());
		} else {
			if (c.getEvents().size() > 0)
				services.eventPublisher().publishEvents(c.isRestore(), c.transactionId(),
						c.eventMetadata(), c.getEvents().toArray());
		}
	};

	protected void interceptors(TransactionStage stage, ProcessorContext context) {
		List<TransactionInterceptor> interceptors = services.interceptorCollection().getInterceptors(stage);
		if (!context.isRestore() && interceptors.size() > 0)
			for (int i = 0; i < interceptors.size(); i++) {
				interceptors.get(i).intercept(context.transactionId(), context.time(), services.repository(), stage);
			}
	}

	protected void rollIfRequired(long transactionId) {
		if (isRollRequired(services.transactionJournaler().currentChannel())) {
			services.journalsManager().roll(transactionId, () -> isRollRequired(services.transactionJournaler().currentChannel()));
		}
	}

	protected boolean isRollRequired(Channel ch) {
		return services.configuration().revenoJournaling().isPreallocated() && ch.size() - ch.position() <= 0;
	}

	protected void changeClassLoaderIfRequired() {
		if (Thread.currentThread().getContextClassLoader() != services.classLoader()) {
			Thread.currentThread().setContextClassLoader(services.classLoader());
		}
	}
	
	
	public InputHandlers(WorkflowContext context, LongSupplier nextTransactionId) {
		this.services = context;
		this.txExecutor = new TransactionExecutor();
		this.nextTransactionId = nextTransactionId;
	}
	
	protected static final Logger log = LoggerFactory.getLogger(InputHandlers.class);
}
