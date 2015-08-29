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

import com.google.common.io.Files;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.reveno.atp.api.Configuration.CpuConsumption;
import org.reveno.atp.core.RevenoConfiguration;
import org.reveno.atp.core.api.EventsCommitInfo;
import org.reveno.atp.core.api.EventsCommitInfo.Builder;
import org.reveno.atp.core.api.Journaler;
import org.reveno.atp.core.api.serialization.EventsInfoSerializer;
import org.reveno.atp.core.data.DefaultJournaler;
import org.reveno.atp.core.engine.processor.PipeProcessor;
import org.reveno.atp.core.events.Event;
import org.reveno.atp.core.events.EventHandlersManager;
import org.reveno.atp.core.events.EventPublisher;
import org.reveno.atp.core.events.EventsContext;
import org.reveno.atp.core.impl.EventsCommitInfoImpl;
import org.reveno.atp.core.serialization.SimpleEventsSerializer;
import org.reveno.atp.core.storage.FileSystemStorage;
import org.reveno.atp.test.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DisruptorEventsBusTest {
	
	private File tempDir;
	private FileSystemStorage storage;
	
	@Before 
	public void setUp() {
		tempDir = Files.createTempDir();
		storage = new FileSystemStorage(tempDir, new RevenoConfiguration.RevenoJournalingConfiguration());
	}
	
	@After 
	public void tearDown() throws IOException {
		FileUtils.delete(tempDir);
	}

	@Test
	public void test() throws InterruptedException {
		final MyEvent[] event = new MyEvent[1];
		CountDownLatch latch = new CountDownLatch(3);
		
		EventHandlersManager manager = new EventHandlersManager();
		manager.eventHandler(MyEvent.class, (e, md) -> { event[0] = e; latch.countDown(); });
		
		manager.eventHandler(MyNextEvent.class, (e, md) -> latch.countDown());
		
		String fileAddress = storage.nextStore().getEventsCommitsAddress();
		Journaler journaler = new DefaultJournaler();
		journaler.startWriting(storage.channel(fileAddress));
		
		EventsCommitInfo.Builder builder = new EventsCommitInfoImpl.PojoBuilder();
		EventsInfoSerializer serializer = new SimpleEventsSerializer();
		
		PipeProcessor<Event> pipe = new DisruptorEventPipeProcessor(CpuConsumption.HIGH, 1024, Executors.newCachedThreadPool());
		EventPublisher eventsBus = new EventPublisher(pipe, new Context(journaler, builder, serializer, manager));
		eventsBus.getPipe().start();
		
		eventsBus.publishEvents(false, 5L, null, new Object[] { new MyEvent("Hello!"), new MyNextEvent() });
		eventsBus.publishEvents(false, 6L, null, new Object[] { new MyNextEvent() });
		
		latch.await(1000, TimeUnit.MILLISECONDS);
		Assert.assertNotNull(event[0]);
		Assert.assertEquals(event[0].message, "Hello!");
		
		eventsBus.getPipe().stop();
		journaler.stopWriting();
		
		System.out.println(new File(tempDir, fileAddress).length());
	}
	
	public static class MyEvent {
		public String message;
		
		public MyEvent(String message) {
			this.message = message;
		}
	}
	
	public static class MyNextEvent {
		
	}
	
	public static class Context implements EventsContext {

		private Journaler journaler;
		@Override
		public Journaler eventsJournaler() {
			return journaler;
		}

		private Builder builder;
		@Override
		public Builder eventsCommitBuilder() {
			return builder;
		}

		private EventsInfoSerializer serializer;
		@Override
		public EventsInfoSerializer serializer() {
			return serializer;
		}

		private EventHandlersManager manager;
		@Override
		public EventHandlersManager manager() {
			return manager;
		}
		
		public Context(Journaler journaler, Builder builder, EventsInfoSerializer serializer, EventHandlersManager manager) {
			this.journaler = journaler;
			this.builder = builder;
			this.serializer = serializer;
			this.manager = manager;
		}
	}
	
}
