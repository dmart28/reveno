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

import org.reveno.atp.acceptance.tests.RevenoBaseTest;
import org.reveno.atp.api.ChannelOptions;
import org.reveno.atp.api.Configuration;
import org.reveno.atp.api.Configuration.CpuConsumption;
import org.reveno.atp.api.Configuration.ModelType;
import org.reveno.atp.api.domain.WriteableRepository;
import org.reveno.atp.api.transaction.TransactionInterceptor;
import org.reveno.atp.api.transaction.TransactionStage;
import org.reveno.atp.core.Engine;

import java.text.DecimalFormat;
import java.util.concurrent.ExecutionException;

public class LatencyTest extends RevenoBaseTest {

	public static final long TOTAL_TRANSACTIONS = 180_000_000;
	public static final long COLD_START_COUNT = 40_000_000;
	
	public static void main(String[] args) throws Exception {
		LatencyTest test = new LatencyTest();
		test.modelType = ModelType.IMMUTABLE;
		if (args[0].equals("low")) {
			test.setUp();
			test.testLow(args[1]);
			test.tearDown();
		}

		if (args[0].equals("normal")) {
			test.setUp();
			test.testNormal(args[1]);
			test.tearDown();
		}

		if (args[0].equals("high")) {
			test.setUp();
			test.testHigh(args[1]);
			test.tearDown();
		}

		if (args[0].equals("phased")) {
			test.setUp();
			test.testPhased(args[1]);
			test.tearDown();
		}
	}

	public void testLow(String type) throws Exception {
		log.info("Testing with LOW consumption:");
		doTest(CpuConsumption.LOW, type);
	}

	public void testNormal(String type) throws Exception {
		log.info("Testing with NORMAL consumption:");
		doTest(CpuConsumption.NORMAL, type);
	}

	public void testHigh(String type) throws Exception {
		log.info("Testing with HIGH consumption:");
		doTest(CpuConsumption.HIGH, type);
	}

	public void testPhased(String type) throws Exception {
		log.info("Testing with PHASED consumption:");
		doTest(CpuConsumption.PHASED, type);
	}

	private void doTest(CpuConsumption consumption, String type) throws InterruptedException, ExecutionException {
		final long[] counter = { 0L };
		final long[] latency = { 0L };
		final long[] worstLatency = { 0L };
		final long[] worstCount = { 0L };
		final long[] bestCount = { 0L };
		Engine engine = createEngine(e -> {
			e.domain().command(CreateAccount.class, Long.class, (c, ctx) -> {
				c.id = ctx.id(Account.class);
				ctx.executeTransaction(c);
				return c.id;
			});
			e.domain().transactionAction(CreateAccount.class, (t, ctx) -> {
				ctx.repo().store(t.id, new Account(t.id));
			});
			e.domain().command(AddBalance.class, (t, ctx) -> {
				ctx.executeTransaction(t);
			});
			e.domain().transactionAction(AddBalance.class, (t, ctx) -> {
				ctx.repo().store(t.acc, ctx.repo().get(Account.class, t.acc).add(t.amount));
			});
			//e.config().mutableModel();
			//e.config().mutableModelFailover(Configuration.MutableModelFailover.COMPENSATING_ACTIONS);
			e.config().mapCapacity(1024);

			e.config().cpuConsumption(consumption);
			e.config().journaling().volumes(8);
			e.config().journaling().minVolumes(0);
			e.config().journaling().volumesSize(1 * 1024 * 1024 * 1024L, 1024);
			if (type.equals("buffering")) {
				e.config().journaling().channelOptions(ChannelOptions.BUFFERING_VM);
			} else {
				e.config().journaling().channelOptions(ChannelOptions.BUFFERING_MMAP_OS);
			}
			e.interceptors().add(TransactionStage.TRANSACTION, new TransactionInterceptor() {
				@Override
				public void destroy() {
				}

				@Override
				public void intercept(long transactionId, long time, WriteableRepository repository, TransactionStage stage) {
					if (++counter[0] > COLD_START_COUNT && transactionId < TOTAL_TRANSACTIONS - 5) {
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
				}
			});
		}, false);
		engine.startup();
		
		final long accountId = engine.executeSync(new CreateAccount());
		
		AddBalance cmd = new AddBalance(accountId, 10);
		Thread[] ths = new Thread[threadCount()];
		for (int a = 0; a < ths.length; a++) {
			Thread t1 = new Thread(() -> {
				try {
				for (long j = 1; j < TOTAL_TRANSACTIONS / 200_000; j++) {
					for (long i = 1; i <= 200_000; i++) {
						engine.executeCommand(cmd);
					}
				}
				} catch (Throwable t) {
					t.printStackTrace();
				}
				});
				t1.start();
				ths[a] = t1;
		}
		
		for (int a = 0; a < ths.length; a++) {
			ths[a].join();
		}

		engine.executeSync(cmd);

		log.info("Avg latency: " + (int)(((double)latency[0] / ((TOTAL_TRANSACTIONS) * threadCount() - COLD_START_COUNT))) + " nanos");
		log.info("Worst latency: " + new DecimalFormat("##.###").format(worstLatency[0] / 1_000_000d) + " millis");
		log.info("Bad perecent (>3mls): " +
				new DecimalFormat("##.########").format((((double)worstCount[0] / (TOTAL_TRANSACTIONS * threadCount() - COLD_START_COUNT)) * 100)) + "%");
		log.info("Best perecent (<1mcr): " +
				new DecimalFormat("##.########").format((((double)bestCount[0] / (TOTAL_TRANSACTIONS * threadCount() - COLD_START_COUNT)) * 100)) + "%");
		
		engine.shutdown();
	}

	public static class Account {
		public final long id;
		public long balance;

		public Account add(long amount) {
			return new Account(balance + amount);
		}

		public Account(long id) {
			this.id = id;
		}
	}

	public static class CreateAccount {
		public long id;
	}

	public static class AddBalance {
		public final long acc;
		public final long amount;

		public AddBalance(long acc, long amount) {
			this.acc = acc;
			this.amount = amount;
		}
	}

    private int threadCount() {
        return Math.max(1, Runtime.getRuntime().availableProcessors() / 4);
    }

}
