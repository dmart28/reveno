package org.reveno.atp.metrics.meter;

public interface Counter extends Snapshotable {

	void inc(long i);
	
	void inc(int i);
	
	void inc();
	
}
