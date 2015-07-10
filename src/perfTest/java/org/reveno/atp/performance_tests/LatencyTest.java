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

package org.reveno.atp.performance_tests;

import java.text.DecimalFormat;

import org.junit.Test;
import org.reveno.atp.acceptance.api.commands.CreateNewAccountCommand;
import org.reveno.atp.acceptance.api.transactions.Credit;
import org.reveno.atp.acceptance.tests.RevenoBaseTest;
import org.reveno.atp.api.Configuration.ModelType;
import org.reveno.atp.api.transaction.TransactionStage;
import org.reveno.atp.core.Engine;

public class LatencyTest extends RevenoBaseTest {

	public static final long TOTAL_TRANSACTIONS = 103_000_000;
	public static final long COLD_START_COUNT = 10_000_000;
	
	public static void main(String[] args) throws Exception {
		LatencyTest test = new LatencyTest();
		test.modelType = ModelType.IMMUTABLE;
		test.setUp();
		test.test();
		test.tearDown();
	}
	
	@Test
	public void test() throws Exception {
		final long[] counter = { 0L };
		final long[] latency = { 0L };
		final long[] worstLatency = { 0L };
		final long[] worstCount = { 0L };
		final long[] bestCount = { 0L };
		Engine engine = createEngine((e) -> {
			e.interceptors().add(TransactionStage.REPLICATION, (id, time, r, s) -> {
				if (++counter[0] > COLD_START_COUNT) {
					final long lat = System.nanoTime() - time;
					latency[0] += lat;
					if (lat > worstLatency[0]) {
						worstLatency[0] = lat;
					}
					if (lat > 3_000_000) {
						worstCount[0]++;
					}
					if (lat <= 1000) {
						bestCount[0]++;
					}
				}
			});
		});
		engine.startup();
		
		final long accountId = sendCommandSync(engine, new CreateNewAccountCommand("USD", 1000_000L));
		
		Credit cmd = new Credit(accountId, 2, 0L);
		Thread[] ths = new Thread[Runtime.getRuntime().availableProcessors() / 2];
		for (int a = 0; a < ths.length; a++) {
			Thread t1 = new Thread(() -> {
				for (long j = 1; j < TOTAL_TRANSACTIONS / 100_000; j++) {
					for (long i = 1; i <= 100_000; i++) {
						engine.executeCommand(cmd);
					}
				}
				});
				t1.start();
				ths[a] = t1;
		}
		
		for (int a = 0; a < ths.length; a++) {
			ths[a].join();
		}
		
		//sendCommandSync(engine, new Credit(accountId, 2, System.currentTimeMillis()));
		
		log.info("Avg latency: " + ((double)latency[0] / ((TOTAL_TRANSACTIONS) * (Runtime.getRuntime().availableProcessors()/2) - COLD_START_COUNT)));
		log.info("Worst latency: " + worstLatency[0]);
		log.info("Worst precent: " + 
				new DecimalFormat("##.########").format((((double)worstCount[0] / (TOTAL_TRANSACTIONS * (Runtime.getRuntime().availableProcessors()/2) - COLD_START_COUNT)) * 100)) + "%");
		log.info("Best precent: " + 
				new DecimalFormat("##.########").format((((double)bestCount[0] / (TOTAL_TRANSACTIONS * (Runtime.getRuntime().availableProcessors()/2) - COLD_START_COUNT)) * 100)) + "%");
		sleep(2000);
		
		engine.shutdown();
	}
	
	public static class TimeHolder {
		public final long time;
		
		public TimeHolder(long time) {
			this.time = time;
		}
	}
	
}
