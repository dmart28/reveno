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
import org.reveno.atp.api.transaction.TransactionStage;
import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.api.channel.Channel;
import org.reveno.atp.core.channel.ChannelBuffer;
import org.reveno.atp.core.disruptor.ProcessorContext;
import org.reveno.atp.core.engine.components.TransactionExecutor;
import org.reveno.atp.utils.MeasureUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class InputHandlers {

	@SuppressWarnings("unchecked")
	public void ex(ProcessorContext c, boolean filter, boolean eob, BiConsumer<ProcessorContext, Boolean> body) {
		if (!c.isAborted() && filter) {
			try {
				body.accept(c, eob);
			} catch (Throwable t) {
				log.error("inputHandlers", t);
				c.abort(t);
				c.future().complete(new EmptyResult(t));
			}
		}
	}
	
	public void replication(ProcessorContext c, boolean endOfBatch) {
		ex(c, !c.isRestore(), endOfBatch, replicator);
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
					c.future().complete(new Result<Object>(c.commandResult()));
				else
					c.future().complete(new EmptyResult());
			}
		}
	}
	
	public void destroy() {
		marshalled.release();
	}
	
	protected final Buffer marshalled = new ChannelBuffer(ByteBuffer.allocateDirect(MeasureUtils.mb(1)));
	protected WorkflowContext services;
	protected TransactionExecutor txExecutor;
	protected Supplier<Long> nextTransactionId;
	
	protected final BiConsumer<ProcessorContext, Boolean> replicator = (c, eob) -> {
		interceptors(TransactionStage.REPLICATION, c);
		
		if (eob) {
			marshalled.clear();
		} else {
			services.serializer().serializeCommands(c.getCommands(), marshalled);
		}
	};
	protected final BiConsumer<ProcessorContext, Boolean> transactionExecutor = (c, eob) -> {
		if (!c.isRestore())
			c.transactionId(nextTransactionId.get());
		
		interceptors(TransactionStage.TRANSACTION, c);
		txExecutor.executeCommands(c, services);
	};
	protected final BiConsumer<ProcessorContext, Boolean> journaler = (c, eob) -> {
		interceptors(TransactionStage.JOURNALING, c);

		rollIfRequired();
		services.transactionJournaler().writeData(b -> {
			c.commitInfo().transactionId(c.transactionId()).time(c.time()).transactionCommits(c.getTransactions());
			services.serializer().serialize(c.commitInfo(), b);
		}, eob);
	};
	protected final BiConsumer<ProcessorContext, Boolean> viewsUpdater = (c, eob) -> {
		services.viewsProcessor().process(c.getMarkedRecords());
	};
	protected final BiConsumer<ProcessorContext, Boolean> eventsPublisher = (c, eob) -> {
		if (c.getEvents().size() > 0) 
			services.eventPublisher().publishEvents(c.isRestore(), c.transactionId(), c.eventMetadata(), c.getEvents().toArray());
	};
	

	protected void interceptors(TransactionStage stage, ProcessorContext context) {
		if (!context.isRestore() && services.interceptorCollection().getInterceptors(stage).size() > 0)
			services.interceptorCollection().getInterceptors(stage).forEach(i -> i.intercept(context.transactionId(),
					context.time(), services.repository(), stage));
	}

	protected void rollIfRequired() {
		if (isRollRequired(services.transactionJournaler().currentChannel())) {
			services.journalsManager().roll(() -> isRollRequired(services.transactionJournaler().currentChannel()));
		}
	}

	protected boolean isRollRequired(Channel ch) {
		return services.configuration().revenoJournaling().isPreallocated() && ch.size() - ch.position() <= 0;
	}
	
	
	public InputHandlers(WorkflowContext context, Supplier<Long> nextTransactionId) {
		this.services = context;
		this.txExecutor = new TransactionExecutor();
		this.nextTransactionId = nextTransactionId;
	}
	
	protected static final Logger log = LoggerFactory.getLogger(InputHandlers.class);
}
