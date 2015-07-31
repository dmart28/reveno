package org.reveno.atp.metrics.meter.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.reveno.atp.metrics.meter.Counter;

public class SimpleCounter implements Counter {

	@Override
	public Map<String, String> snapshot() {
		Map<String, String> map = new HashMap<>();
		map.put(name + ".hits", Long.toString(counter.getAndSet(0)));
		return map;
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
	protected final AtomicLong counter = new AtomicLong();

}
