/**
 *  Copyright (c) 2015 The original author or authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.reveno.atp.metrics;

import org.reveno.atp.api.transaction.TransactionStage;
import org.reveno.atp.core.Engine;

public class RevenoMetrics {
	
	public Configuration config() {
		return config;
	}
	
	public void listen(Engine engine) {
		if (engine.isClustered()) {
			engine.interceptors().add(TransactionStage.REPLICATION, interceptor);
		} else {
			engine.interceptors().add(TransactionStage.TRANSACTION, interceptor);
		}
		interceptor.init();
	}
	
	public void shutdown(Engine engine) {
		if (engine.isClustered()) {
			engine.interceptors().getInterceptors(TransactionStage.REPLICATION).remove(interceptor);
		} else {
			engine.interceptors().getInterceptors(TransactionStage.TRANSACTION).remove(interceptor);
		}
		interceptor.shutdown();
	}
	
	protected ConfigurationImpl config = new ConfigurationImpl();
	protected MetricsInterceptor interceptor = new MetricsInterceptor(config);
	
}
