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

package org.reveno.atp.metrics.meter.impl;

import org.reveno.atp.metrics.Sink;
import org.reveno.atp.metrics.meter.Counter;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class SimpleCounter implements Counter {

	@Override
	public void sendTo(List<Sink> sinks, boolean sync) {
		long count = counter.getAndSet(0);
		long current = System.currentTimeMillis();
		long interval = (current - lastTime) / (1000);
		if (interval > 0) {
			for (long i = lastTime; i <= current; i += 1000) {
				for (Sink s : sinks) {
					s.send(name + ".hits", Long.toString(count / interval), i / 1000);
				}
			}
		}
		lastTime = current;
	}

	@Override
	public void inc(long i) {
		counter.addAndGet(i);
	}

	@Override
	public void inc(int i) {
		inc((long)i);
	}

	@Override
	public void inc() {
		counter.incrementAndGet();
	}
	
	public SimpleCounter(String name) {
		this.name = name;
	}
	
	protected String name;
	protected long lastTime = System.currentTimeMillis();
	protected final AtomicLong counter = new AtomicLong();

}
