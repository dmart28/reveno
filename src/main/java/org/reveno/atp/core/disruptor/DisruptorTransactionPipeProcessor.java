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

package org.reveno.atp.core.disruptor;

import com.lmax.disruptor.EventFactory;
import org.reveno.atp.api.Configuration.CpuConsumption;
import org.reveno.atp.api.EventsManager.EventMetadata;
import org.reveno.atp.api.commands.EmptyResult;
import org.reveno.atp.api.commands.Result;
import org.reveno.atp.core.api.RestoreableEventBus;
import org.reveno.atp.core.api.TransactionCommitInfo;
import org.reveno.atp.core.engine.processor.TransactionPipeProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class DisruptorTransactionPipeProcessor extends DisruptorPipeProcessor<ProcessorContext> implements TransactionPipeProcessor<ProcessorContext> {

	public DisruptorTransactionPipeProcessor(TransactionCommitInfo.Builder builder, 
			CpuConsumption cpuConsumption, int bufferSize, ExecutorService executor) {
		this.cpuConsumption = cpuConsumption;
		this.bufferSize = bufferSize;
		this.executor = executor;
		this.eventFactory = () -> new ProcessorContext(builder.create());
	}
	
	@Override
	public CpuConsumption cpuConsumption() {
		return cpuConsumption;
	}
	
	@Override
	public int bufferSize() {
		return bufferSize;
	}

	@Override
	boolean singleProducer() {
		return false;
	}

	@Override
	public EventFactory<ProcessorContext> eventFactory() {
		return eventFactory;
	}

	@Override
	public ExecutorService executor() {
		return executor;
	}

	@Override
	public void startupInterceptor() {
		// TODO exception listener that will stop disruptor, mark node Slave, etc.
	}
	
	@Override
	public void sync() {
		CompletableFuture<EmptyResult> res = process((c,f) -> c.reset().future(f).abort(null));
		try {
			res.get();
		} catch (Throwable t) {
			log.error("sync", t);
		}
	}

	@Override
	public CompletableFuture<EmptyResult> process(List<Object> commands) {
		return process((e,f) -> e.reset().future(f).addCommands(commands).time(System.nanoTime()));
	}

	@Override
	public <R> CompletableFuture<Result<? extends R>> execute(Object command) {
		return process((e,f) -> e.reset().future(f).addCommand(command).time(System.nanoTime()).withResult());
	}
	
	@Override
	public void executeRestore(RestoreableEventBus eventBus, TransactionCommitInfo tx) {
		process((e,f) -> e.reset().restore().transactionId(tx.transactionId())
					.eventBus(eventBus).eventMetadata(metadata(tx)).getTransactions()
					.addAll(tx.transactionCommits()));
	}
	
	protected EventMetadata metadata(TransactionCommitInfo tx) {
		return new EventMetadata(true, tx.time());
	}
	
	
	protected final CpuConsumption cpuConsumption;
	protected final int bufferSize;
	protected final ExecutorService executor;
	protected final EventFactory<ProcessorContext> eventFactory;
	private static final Logger log = LoggerFactory.getLogger(DisruptorTransactionPipeProcessor.class);
	
}
