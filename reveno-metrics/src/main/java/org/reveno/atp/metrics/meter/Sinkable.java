package org.reveno.atp.metrics.meter;

import org.reveno.atp.metrics.Sink;

public interface Sinkable {

	void sendTo(Sink sink);
	
}
