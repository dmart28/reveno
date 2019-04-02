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
		long interval = (current - lastTime) / 1000;
		if (interval > 0) {
			for (long i = lastTime; i <= current; i += 1000) {
				for (Sink s : sinks) {
					s.send(metricsName, Long.toString(count / interval), i / 1000);
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
		this.metricsName = name + ".hits";
	}
	
	protected final String name;
	protected final String metricsName;
	protected long lastTime = System.currentTimeMillis();
	protected final AtomicLong counter = new AtomicLong();

}
