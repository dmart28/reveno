package org.reveno.atp.metrics.meter;

import org.reveno.atp.metrics.Sink;

public interface Sinkable {

	/**
	 * Sends the last snapshot of metrics to some {@link Sink}.
	 * 
	 * @param sink pipe into which last metrics will be loaded
	 * @param sync whether to await while it will be safe to catch results ({@value true} make sense only in multithreaded environment)
	 */
	void sendTo(Sink sink, boolean sync);
	
}
