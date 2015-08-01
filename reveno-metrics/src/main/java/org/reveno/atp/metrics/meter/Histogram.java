package org.reveno.atp.metrics.meter;

import org.reveno.atp.core.api.Destroyable;

public interface Histogram extends Sinkable, Destroyable {

	Histogram update(long value);
	
	boolean isReady();
	
}
