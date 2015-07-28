package org.reveno.atp.metrics;

import org.reveno.atp.api.transaction.TransactionStage;
import org.reveno.atp.core.Engine;

public class RevenoMetrics {
	
	public Configuration config() {
		return config;
	}
	
	public void connectTo(Engine engine) {
		engine.interceptors().add(TransactionStage.REPLICATION, interceptor);
		interceptor.init();
	}
	
	protected Configuration config = new Configuration();
	protected MetricsInterceptor interceptor = new MetricsInterceptor(config);
	
}
