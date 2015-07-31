package org.reveno.atp.metrics.meter;

public interface Histogram extends Snapshotable {

	void update(long value, long time);
	
	boolean isReady();
	
}
