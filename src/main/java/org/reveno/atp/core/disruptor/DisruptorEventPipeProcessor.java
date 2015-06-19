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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.reveno.atp.api.Configuration.CpuConsumption;
import org.reveno.atp.core.events.Event;
import org.reveno.atp.core.events.EventPublisher;

import com.lmax.disruptor.EventFactory;

public class DisruptorEventPipeProcessor extends DisruptorPipeProcessor<Event> {
	
	public DisruptorEventPipeProcessor(CpuConsumption cpuConsumption, Executor executor) {
		this.cpuConsumption = cpuConsumption;
		this.executor = executor;
	}

	@Override
	public void sync() {
		CompletableFuture<?> res = process((e,f) -> e.reset().flag(EventPublisher.SYNC_FLAG).syncFuture(f));
		try {
			res.get();
		} catch (Throwable t) {
			log.error("sync", t);
		}
	}

	@Override
	public CpuConsumption cpuConsumption() {
		return cpuConsumption;
	}

	@Override
	boolean singleProducer() {
		return true;
	}

	@Override
	public EventFactory<Event> eventFactory() {
		return eventFactory;
	}

	@Override
	public Executor executor() {
		return executor;
	}

	@Override
	void startInterceptor() {
	}

	protected final CpuConsumption cpuConsumption;
	protected final Executor executor;
	protected static final EventFactory<Event> eventFactory = () -> new Event();
	
}
