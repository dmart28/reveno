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

package org.reveno.atp.core.events;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.reveno.atp.api.Configuration.CpuConsumption;
import org.reveno.atp.api.EventsManager.EventMetadata;
import org.reveno.atp.api.commands.EmptyResult;
import org.reveno.atp.core.api.EventPublisher;
import org.reveno.atp.core.api.EventsCommitInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.PhasedBackoffWaitStrategy;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

public class DisruptorEventPublisher implements EventPublisher {
	
	protected EventsContext context;

	public DisruptorEventPublisher(CpuConsumption cpuConsumption, EventsContext context) {
		this.cpuConsumption = cpuConsumption;
		this.context = context;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void start() {
		if (isStarted) throw new IllegalStateException("The Event Bus is alredy started.");
		
		disruptor = new Disruptor<Event>(eventFactory, 8 * 1024, executor, ProducerType.MULTI, createWaitStrategy());
		disruptor.handleEventsWith(this::publish).then(this::serialize).then(this::journal);
		disruptor.start();
		
		log.info("Started.");
		isStarted = true;
	}
	
	@Override
	public void stop() {
		requireStarted();
		
		isStarted = false;
		disruptor.shutdown();
		log.info("Stopped.");
	}
	
	@Override
	public void sync() {
		requireStarted();
		
		final CompletableFuture<EmptyResult> f = new CompletableFuture<EmptyResult>();
		disruptor.publishEvent((e,s) -> e.reset().flag(SYNC_FLAG).syncFuture(f));
		try {
			f.get();
		} catch (Throwable t) {
			log.error("sync", t);
		}
	}
	
	@Override
	public void shutdown() {
		disruptor.shutdown();
		
		for (int i = 0; i < 8 * 1024; i++)
			disruptor.getRingBuffer().get(i).destroy();
	}
	
	protected void ex(Event c, boolean filter, boolean eob, BiConsumer<Event, Boolean> body) {
		if (!c.isAborted() && filter) {
			try {
				body.accept(c, eob);
			} catch (Throwable t) {
				log.error("eventsBus", t);
				c.abort();
			}
		}
	}
	
	@Override
	public void publishEvents(boolean isReplay, long transactionId, EventMetadata metadata, Object[] events) {
		requireStarted(() -> {
			disruptor.publishEvent((e,s) -> e.reset().replay(isReplay).eventMetadata(metadata)
					.transactionId(transactionId).events(events));
			return null;
		});
	}
	
	@Override
	public void commitAsyncError(boolean isReplay, long transactionId) {
		requireStarted(() -> {
			disruptor.publishEvent((e,s) -> e.reset().flag(ASYNC_ERROR_FLAG).replay(isReplay).transactionId(transactionId));
			return null;
		});
	}
	
	protected void publish(Event event, long sequence, boolean endOfBatch) {
		ex(event, event.getFlag() == 0, endOfBatch, publisher);
	}
	
	protected void serialize(Event event, long sequence, boolean endOfBatch) {
		ex(event, event.getFlag() != SYNC_FLAG, endOfBatch, serializer);
	}
	
	protected void journal(Event event, long sequence, boolean endOfBatch) {
		ex(event, true, endOfBatch, journaler);
	}
	
	protected final BiConsumer<Event, Boolean> publisher = (e, eof) -> {
		boolean needAsync = false;
		for (Object event : e.events()) {
			context.manager().getEventHandlers(event.getClass()).forEach(h -> h.accept(event,
					e.eventMetadata() == null ? metadata : e.eventMetadata()));
			needAsync = context.manager().getAsyncHandlers(event.getClass()).size() > 0;
		}
		if (needAsync) {
			Barrier barrier = new Barrier(this, e.transactionId(), e.isReplay());
			ExecutorService executor = context.manager().asyncEventExecutor();
			for (Object event : e.events()) {
				if (context.manager().getEventHandlers(event.getClass()).size() == 0) {
					context.manager().getAsyncHandlers(event.getClass()).forEach(h -> {
						barrier.open();
						executor.execute(() -> {
							try {
								h.accept(event, e.eventMetadata() == null ? metadata : e.eventMetadata());
							} catch (Throwable t) {
								log.error("asyncEventExecutor", t);
								barrier.fail();
							}
						});
					});
				}
			}
			if (barrier.isOpen()) {
				executor.execute(barrier);
			}
		}
	};
	
	protected void requireStarted() {
		if (!isStarted) throw new IllegalStateException("The Events Bus is already stopped.");
	}
	
	protected final BiConsumer<Event, Boolean> serializer = (e, eof) -> {
		EventsCommitInfo info = context.eventsCommitBuilder().create(e.transactionId(), System.currentTimeMillis(), 
				e.getFlag());
		context.serializer().serialize(info, e.serialized());
	};
	
	protected final BiConsumer<Event, Boolean> journaler = (e, eof) -> {
		if (e.getFlag() == SYNC_FLAG) {
			e.syncFuture().complete(null);
		} else {
			context.eventsJournaler().writeData(e.serialized(), eof);
		}
	};
	
	protected WaitStrategy createWaitStrategy() {
		switch (cpuConsumption) {
		case LOW:
			return new BlockingWaitStrategy();
		case NORMAL:
			return new SleepingWaitStrategy();
		case HIGH:
			return new YieldingWaitStrategy();
		case PHASED:
			return PhasedBackoffWaitStrategy.withLock((int) 2.5e5, (int) 8.5e5, TimeUnit.NANOSECONDS);
		}
		return null;
	}
	
	void requireStarted(Supplier<Void> body) {
		if (isStarted)
			body.get();
		else
			throw new IllegalStateException(
					"Events Bus must be started first.");
	}
	
	protected static final EventMetadata metadata = new EventMetadata(false, 0) {
		public long getTransactionTime() {
			return System.currentTimeMillis();
		};
	};
	
	protected volatile boolean isStarted = false;
	protected Disruptor<Event> disruptor;
	protected final Executor executor = Executors.newFixedThreadPool(3);
	protected final CpuConsumption cpuConsumption;
	protected static final EventFactory<Event> eventFactory = () -> new Event();
	private static final Logger log = LoggerFactory.getLogger(DisruptorEventPublisher.class);
}
