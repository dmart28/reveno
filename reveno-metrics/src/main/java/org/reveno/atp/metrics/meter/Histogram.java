package org.reveno.atp.metrics.meter;

public interface Histogram extends Sinkable {

	void update(long value, long time);
	
	boolean isReady();
	
}
