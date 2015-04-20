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

import java.util.function.BiConsumer;

import org.reveno.atp.api.commands.EmptyResult;
import org.reveno.atp.api.commands.Result;
import org.reveno.atp.core.api.TransactionCommitInfo;
import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.channel.NettyBasedBuffer;
import org.reveno.atp.core.engine.components.SerializersChain;
import org.reveno.atp.core.engine.components.TransactionExecutor;
import org.reveno.atp.core.engine.processor.ProcessorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	
	public void marshalling(ProcessorContext c, boolean endOfBatch) {
		ex(c, !c.isReplicated() && !c.isReplay(), endOfBatch, marshaller);
	}
	
	public void replication(ProcessorContext c, boolean endOfBatch) {
		ex(c, c.marshallerBuffer().length() > 0 && !c.isReplay(), endOfBatch, replicator);
	}
	
	public void transactionExecution(ProcessorContext c, boolean endOfBatch) {
		ex(c, true, endOfBatch, transactionExecutor);
	}
	
	public void serialization(ProcessorContext c, boolean endOfBatch) {
		ex(c, c.getTransactions().size() > 0 && !c.isReplay(), endOfBatch, serializator);
	}
	
	public void journaling(ProcessorContext c, boolean endOfBatch) {
		ex(c, !c.isReplay(), endOfBatch, journaler);
	}
	
	public void viewsUpdate(ProcessorContext c, boolean endOfBatch) {
		ex(c, true, endOfBatch, viewsUpdater);
	}
	
	public void eventsPublishing(ProcessorContext c, boolean endOfBatch) {
		ex(c, c.getEvents().size() > 0, endOfBatch, eventsPublisher);
	}
	
	@SuppressWarnings("unchecked")
	public void result(ProcessorContext c, boolean endOfBatch) {
		if (!c.isReplay()) {
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
	
	protected WorkflowContext services;
	protected SerializersChain serializer;
	protected TransactionExecutor txExecutor;
	
	protected final BiConsumer<ProcessorContext, Boolean> marshaller = (c, eob) -> {
		serializer.serializeCommands(c.getCommands(), c.marshallerBuffer());
	};
	protected final BiConsumer<ProcessorContext, Boolean> replicator = (c, eob) -> {
		if (eob) {
			// TODO some replication here
		} else {
			marshalled.writeInt(c.marshallerBuffer().length());
			marshalled.writeFromBuffer(c.marshallerBuffer());
		}
	};
	protected final BiConsumer<ProcessorContext, Boolean> transactionExecutor = (c, eob) -> {
		txExecutor.executeCommands(c, services);
	};
	protected final BiConsumer<ProcessorContext, Boolean> serializator = (c, eob) -> {
		// TODO what's with version?
		TransactionCommitInfo tci = services.transactionCommitBuilder().create(services.nextTransactionId(), 1,
				System.currentTimeMillis(), c.getTransactions().toArray());
		serializer.serialize(tci, c.transactionsBuffer());
	};
	protected final BiConsumer<ProcessorContext, Boolean> journaler = (c, eob) -> {
		services.transactionJournaler().writeData(c.transactionsBuffer(), eob);
	};
	protected final BiConsumer<ProcessorContext, Boolean> viewsUpdater = (c, eob) -> {
		services.viewsProcessor().process(c.getMarkedRecords());
	};
	protected final BiConsumer<ProcessorContext, Boolean> eventsPublisher = (c, eob) -> {
		services.eventPublisher().publishEvents(c.transactionId(), c.getEvents().toArray());
	};
	
	
	public InputHandlers(WorkflowContext context) {
		this.services = context;
		this.serializer = new SerializersChain(context.serializers());
		this.txExecutor = new TransactionExecutor();
	}
	
	protected static final Buffer marshalled = new NettyBasedBuffer(true);
	protected static final Logger log = LoggerFactory.getLogger(InputHandlers.class);
}
