package org.reveno.atp.metrics.impl;

import org.reveno.atp.metrics.Sink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class Slf4jSink implements Sink {

	@Override
	public void init() {
	}

	@Override
	public void send(String name, String value, long timestamp) {
		System.out.println("[" + new Date(timestamp * 1000) + "] " + name + ": " + value);
	}

	@Override
	public void close() {
	}

	@Override
	public boolean isAvailable() {
		return true;
	}

	
	protected static Logger log = LoggerFactory.getLogger(Slf4jSink.class);
	
}
