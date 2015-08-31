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

import org.reveno.atp.api.EventsManager.EventMetadata;
import org.reveno.atp.core.api.EventsCommitInfo;
import org.reveno.atp.core.engine.processor.PipeProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;

public class EventPublisher {

	public static final int ASYNC_ERROR_FLAG = 0xBABECAFE;
	public static final int SYNC_FLAG = 0xCAFEBABE;
	
	protected EventsContext context;

	@SuppressWarnings("unchecked")
	public EventPublisher(PipeProcessor<Event> pipeProcessor, EventsContext context) {
		this.pipeProcessor = pipeProcessor;
		this.context = context;
		
		this.pipeProcessor.pipe(this::publish).then(this::journal);
	}
	
	public PipeProcessor<Event> getPipe() {
		return pipeProcessor;
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
	
	public void publishEvents(boolean isReplay, long transactionId, EventMetadata metadata, Object[] events) {
		if (events.length > 0) {
			this.pipeProcessor.process((e,f) -> e.reset().replay(isReplay).eventMetadata(metadata)
					.transactionId(transactionId).events(events));
		}
	}
	
	public void commitAsyncError(boolean isReplay, long transactionId) {
		this.pipeProcessor.process((e,f) -> e.reset().flag(ASYNC_ERROR_FLAG).replay(isReplay).transactionId(transactionId));
	}
	
	protected void publish(Event event, boolean endOfBatch) {
		ex(event, event.getFlag() == 0, endOfBatch, publisher);
	}
	
	protected void journal(Event event, boolean endOfBatch) {
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
	
	protected final BiConsumer<Event, Boolean> journaler = (e, eof) -> {
		if (e.getFlag() == SYNC_FLAG) {
			e.syncFuture().complete(null);
		} else {
			context.eventsJournaler().writeData(b -> {
				EventsCommitInfo info = context.eventsCommitBuilder().create(e.transactionId(), System.currentTimeMillis(), 
						e.getFlag());
				context.serializer().serialize(info, b);
			}, eof);
		}
	};
	
	protected static final EventMetadata metadata = new EventMetadata(false, 0) {
		public long getTransactionTime() {
			return System.currentTimeMillis();
		};
	};
	
	protected PipeProcessor<Event> pipeProcessor;
	private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);
}
