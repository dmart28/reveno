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

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.reveno.atp.api.Configuration.CpuConsumption;
import org.reveno.atp.api.EventsManager.EventMetadata;
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

public class DisruptorEventsBus implements EventPublisher {
	
	protected EventsContext context;

	public DisruptorEventsBus(CpuConsumption cpuConsumption, EventsContext context) {
		this.cpuConsumption = cpuConsumption;
		this.context = context;
	}
	
	@SuppressWarnings("unchecked")
	public void start() {
		if (isStarted) throw new IllegalStateException("The Event Bus is alredy started.");
		
		disruptor = new Disruptor<Event>(eventFactory, 8 * 1024, executor, ProducerType.SINGLE, createWaitStrategy());
		disruptor.handleEventsWith(this::publish).then(this::serialize).then(this::journal);
		disruptor.start();
		
		log.info("Started.");
		isStarted = true;
	}
	
	public void stop() {
		if (!isStarted) throw new IllegalStateException("The Events Bus is already stopped.");
		
		isStarted = false;
		disruptor.shutdown();
		log.info("Stopped.");
	}
	
	protected void ex(Event c, boolean eob, BiConsumer<Event, Boolean> body) {
		if (!c.isAborted()) {
			try {
				body.accept(c, eob);
			} catch (Throwable t) {
				log.error("eventsBus", t);
				c.abort();
			}
		}
	}
	
	@Override
	public void publishEvents(long transactionId, Object[] events) {
		requireStarted(() -> {
			disruptor.publishEvent((e,s) -> e.reset().transactionId(transactionId).events(events));
			return null;
		});
	}
	
	protected void publish(Event event, long sequence, boolean endOfBatch) {
		ex(event, endOfBatch, publisher);
	}
	
	protected void serialize(Event event, long sequence, boolean endOfBatch) {
		ex(event, endOfBatch, serializer);
	}
	
	protected void journal(Event event, long sequence, boolean endOfBatch) {
		ex(event, endOfBatch, journaler);
	}
	
	protected final BiConsumer<Event, Boolean> publisher = (e, eof) -> {
		for (Object event : e.events()) {
			context.manager().getEventHandlers(event.getClass()).forEach(h -> h.accept(event, metadata));
		}
		// TODO async processing
	};
	
	protected final BiConsumer<Event, Boolean> serializer = (e, eof) -> {
		EventsCommitInfo info = context.eventsCommitBuilder().create(e.transactionId(), System.currentTimeMillis());
		context.serializer().serialize(info, e.serialized());
	};
	
	protected final BiConsumer<Event, Boolean> journaler = (e, eof) -> {
		context.eventsJournaler().writeData(e.serialized(), eof);
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
	private static final Logger log = LoggerFactory.getLogger(DisruptorEventsBus.class);
}
