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

import org.reveno.atp.api.Configuration.ModelType;
import org.reveno.atp.core.disruptor.ProcessorContext;
import org.reveno.atp.core.engine.processor.TransactionPipeProcessor;
import org.reveno.atp.core.events.EventPublisher;

@SuppressWarnings("unchecked")
public class WorkflowEngine {
	
	public WorkflowEngine(TransactionPipeProcessor<ProcessorContext> inputProcessor, WorkflowContext context,
			ModelType modelType) {
		this.modelType = modelType;
		this.inputProcessor = inputProcessor;
		this.handlers = new InputHandlers(context, this::nextTransactionId);
	}
	
	public void init() {
		if (modelType == ModelType.MUTABLE) {
			inputProcessor.pipe(handlers::replication)
			.then((c, eof) -> {
				handlers.transactionExecution(c, eof);
				if (!c.isAborted())
					handlers.viewsUpdate(c, eof);
			})
			.then(handlers::journaling)
			.then(handlers::result, handlers::eventsPublishing);
		} else {
			inputProcessor.pipe(handlers::replication)
			.then(handlers::transactionExecution)
			.then(handlers::journaling, handlers::viewsUpdate)
			.then(handlers::result, handlers::eventsPublishing);
		}
		inputProcessor.start();
	}
	
	public void shutdown() {
		inputProcessor.shutdown();
		handlers.destroy();
	}
	
	public TransactionPipeProcessor<ProcessorContext> getPipe() {
		return inputProcessor;
	}

	public long getLastTransactionId() {
		return lastTransactionId;
	}

	public void setLastTransactionId(long lastTransactionId) {
		this.lastTransactionId = lastTransactionId;
	}
	
	protected long nextTransactionId() {
		return ++lastTransactionId;
	}
	
	// TODO All failover stuff regulation goes here for #32251622346055
	
	protected volatile long lastTransactionId;
	protected ModelType modelType;
	protected TransactionPipeProcessor<ProcessorContext> inputProcessor;
	protected EventPublisher eventBus;
	protected final InputHandlers handlers;
	
}
