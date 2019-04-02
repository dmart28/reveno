package org.reveno.atp.metrics;

import org.reveno.atp.api.transaction.TransactionStage;
import org.reveno.atp.core.Engine;

public class RevenoMetrics {
	
	public Configuration config() {
		return config;
	}
	
	public void listen(Engine engine) {
		engine.interceptors().add(TransactionStage.TRANSACTION, interceptor);
		interceptor.init();
	}
	
	public void shutdown(Engine engine) {
		engine.interceptors().getInterceptors(TransactionStage.TRANSACTION).remove(interceptor);
		interceptor.shutdown();
	}
	
	protected ConfigurationImpl config = new ConfigurationImpl();
	protected MetricsInterceptor interceptor = new MetricsInterceptor(config);
	
}
