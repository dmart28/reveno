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
	public void ex(ProcessorContext c, boolean filter, boolean eob,
				   BoolBiConsumer<ProcessorContext> body) {
		ex(c, filter, eob, null, null, body);
	}

	@SuppressWarnings("unchecked")
	public void ex(ProcessorContext c, boolean filter, boolean eob,
				   TransactionStage stage, List<TransactionInterceptor> interceptors,
				   BoolBiConsumer<ProcessorContext> body) {
		if (c.isSystem() || (!c.isAborted() && filter)) {
			try {
				if (c.isSystem()) {
					c.transactionId(transactionId.getAsLong());
				}
				if (stage != null && interceptors != null) {
					interceptors(stage, interceptors, c);
				}
				if (!c.isSystem()) {
					body.accept(c, eob);
				}
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
		ex(c, !c.isRestore() && !c.isSync() && !c.isReplicated(), endOfBatch,
				TransactionStage.REPLICATION, replicationInterceptors, replicator);
	}
	
	public void transactionExecution(ProcessorContext c, boolean endOfBatch) {
		if (!c.isRestore() && !c.isSync())
			c.transactionId(nextTransactionId.getAsLong());

		ex(c, !c.isSync(), endOfBatch, TransactionStage.TRANSACTION, transactionInterceptors,
				transactionExecutor);
	}
	
	public void journaling(ProcessorContext c, boolean endOfBatch) {
		ex(c, !c.isSync() && c.getTransactions().size() > 0 && !c.isRestore(), endOfBatch,
				TransactionStage.JOURNALING, journalingInterceptors, journaler);
	}
	
	public void viewsUpdate(ProcessorContext c, boolean endOfBatch) {
		ex(c, !c.isSync(), endOfBatch, viewsUpdater);
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
	protected LongSupplier transactionId;
	protected ProcessorContext ctxJ;
	protected List<TransactionInterceptor> replicationInterceptors;
	protected List<TransactionInterceptor> transactionInterceptors;
	protected List<TransactionInterceptor> journalingInterceptors;

	protected final Consumer<Buffer> journalerConsumer = b -> {
		ctxJ.commitInfo().transactionId(ctxJ.transactionId()).time(ctxJ.time()).transactionCommits(ctxJ.getTransactions());
		services.serializer().serialize(ctxJ.commitInfo(), b);
	};

	private boolean changedClassLoaderReplicator = false;
	protected final BoolBiConsumer<ProcessorContext> replicator = (c, eob) -> {
		if (!changedClassLoaderReplicator) {
			changeClassLoaderIfRequired();
			changedClassLoaderReplicator = true;
		}

		// TODO probably it's better to send generated transactionId by master node
		if (!services.failoverManager().replicate(b -> services.serializer().serializeCommands(c.getCommands(), b)))
			c.abort(new ReplicationFailedException());
	};
	protected final BoolBiConsumer<ProcessorContext> transactionExecutor = (c, eob) -> {
		txExecutor.executeCommands(c, services);
	};
	private boolean changedClassLoaderJournaler = false;
	protected final BoolBiConsumer<ProcessorContext> journaler = (c, eob) -> {
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
			if (c.getEvents().size() > 0) {
				services.eventPublisher().publishEvents(c.isRestore(), c.transactionId(),
						c.eventMetadata(), c.getEvents().toArray());
			}
		}
	};

	protected void interceptors(TransactionStage stage, List<TransactionInterceptor> interceptors, ProcessorContext c) {
		if (!c.isRestore() && interceptors.size() > 0)
			for (int i = 0; i < interceptors.size(); i++) {
				interceptors.get(i).intercept(c.transactionId(), c.time(), c.systemFlag(), services.repository(), stage);
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
	
	
	public InputHandlers(WorkflowContext context, LongSupplier nextTransactionId, LongSupplier transactionId) {
		this.services = context;
		this.txExecutor = new TransactionExecutor();
		this.nextTransactionId = nextTransactionId;
		this.transactionId = transactionId;

		replicationInterceptors = context.interceptorCollection().getInterceptors(TransactionStage.REPLICATION);
		transactionInterceptors = context.interceptorCollection().getInterceptors(TransactionStage.TRANSACTION);
		journalingInterceptors = context.interceptorCollection().getInterceptors(TransactionStage.JOURNALING);
	}
	
	protected static final Logger log = LoggerFactory.getLogger(InputHandlers.class);
}
