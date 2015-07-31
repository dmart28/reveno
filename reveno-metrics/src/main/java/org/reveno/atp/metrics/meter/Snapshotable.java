package org.reveno.atp.metrics.meter;

import java.util.Map;

public interface Snapshotable {

	Map<String, String> snapshot();
	
}
