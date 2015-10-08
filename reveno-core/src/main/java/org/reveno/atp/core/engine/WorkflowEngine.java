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
import org.reveno.atp.api.commands.EmptyResult;
import org.reveno.atp.api.commands.Result;
import org.reveno.atp.api.exceptions.FailoverRulesException;
import org.reveno.atp.core.api.FailoverManager;
import org.reveno.atp.core.api.RestoreableEventBus;
import org.reveno.atp.core.api.TransactionCommitInfo;
import org.reveno.atp.core.disruptor.ProcessorContext;
import org.reveno.atp.core.engine.processor.PipeProcessor;
import org.reveno.atp.core.engine.processor.ProcessorHandler;
import org.reveno.atp.core.engine.processor.TransactionPipeProcessor;
import org.reveno.atp.core.events.EventPublisher;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public class WorkflowEngine {
	
	public WorkflowEngine(TransactionPipeProcessor<ProcessorContext> inputProcessor, WorkflowContext context,
			ModelType modelType) {
		this.modelType = modelType;
		this.context = context;
		this.inputProcessor = new PipeProcessorFailoverWrapper(inputProcessor);
		this.handlers = new InputHandlers(context, this::nextTransactionId);
	}
	
	public void init() {
		PipeProcessor<ProcessorContext> pipe;
		if (context.failoverManager().isSingleNode()) {
			pipe = inputProcessor;
		} else {
			pipe = inputProcessor.pipe(handlers::replication);
		}
		if (modelType == ModelType.MUTABLE) {
			pipe.then((c, eof) -> {
				handlers.transactionExecution(c, eof);
				if (!c.isAborted())
					handlers.viewsUpdate(c, eof);
			})
			.then(handlers::journaling)
			.then(handlers::result, handlers::eventsPublishing);
		} else {
			pipe.then(handlers::transactionExecution)
			.then(handlers::journaling, handlers::viewsUpdate)
			.then(handlers::result, handlers::eventsPublishing);
		}
		inputProcessor.start();
		context.failoverManager().addOnBlocked(() -> {
			getPipe().sync();
			context.eventPublisher().getPipe().sync();
		});
		context.failoverManager().addOnUnblocked(() -> {
			context.journalsManager().roll(getLastTransactionId());
		});
		context.failoverManager().onReplicationMessage(b -> {
			inputProcessor.executeFailover(context.serializer().deserializeCommands(b));
		});

		started = true;
	}
	
	public void shutdown() {
		started = false;

		context.failoverManager().processPendingMessages();
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
	
	protected volatile long lastTransactionId;
	protected volatile boolean started = false;
	protected ModelType modelType;
	protected WorkflowContext context;
	protected PipeProcessorFailoverWrapper inputProcessor;
	protected final InputHandlers handlers;

	protected class PipeProcessorFailoverWrapper implements TransactionPipeProcessor<ProcessorContext> {

		@Override
		public CompletableFuture<EmptyResult> process(List<Object> commands) {
			return execute(() -> pipe.process(commands));
		}

		@Override
		public <R> CompletableFuture<Result<R>> execute(Object command) {
			return execute(() -> pipe.execute(command));
		}

		public void executeFailover(List<Object> commands) {
			if (!failoverManager().isMaster()) {
				pipe.process((c, f) -> {
					c.reset().addCommands(commands).replicated().future(f);
				});
			}
		}

		@Override
		public <R> CompletableFuture<R> process(BiConsumer<ProcessorContext, CompletableFuture<R>> consumer) {
			return execute(() -> pipe.process(consumer));
		}

		@Override
		public void executeRestore(RestoreableEventBus eventBus, TransactionCommitInfo transaction) {
			pipe.executeRestore(eventBus, transaction);
		}

		@Override
		public void start() {
			pipe.start();
		}

		@Override
		public void stop() {
			pipe.stop();
		}

		@Override
		public void sync() {
			pipe.sync();
		}

		@Override
		public void shutdown() {
			pipe.shutdown();
		}

		@Override
		public boolean isStarted() {
			return pipe.isStarted();
		}

		@Override
		public PipeProcessor<ProcessorContext> pipe(ProcessorHandler<ProcessorContext>... handler) {
			return pipe.pipe(handler);
		}

		protected <R> R execute(Supplier<R> r) {
			if (!failoverManager().isBlocked() && failoverManager().isMaster() && started) {
				return r.get();
			} else {
				throw failoverError();
			}
		}

		protected FailoverRulesException failoverError() {
			return new FailoverRulesException(String.format("Pipeline not available [master: %s;blocked: %s;started: %s]",
					failoverManager().isMaster(), failoverManager().isBlocked(), started));
		}

		protected FailoverManager failoverManager() {
			return context.failoverManager();
		}

		public PipeProcessorFailoverWrapper(TransactionPipeProcessor<ProcessorContext> pipe) {
			this.pipe = pipe;
		}

		protected TransactionPipeProcessor<ProcessorContext> pipe;
	}

}
