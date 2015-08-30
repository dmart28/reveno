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

package org.reveno.atp.acceptance.tests;

import org.junit.Assert;
import org.junit.Test;
import org.reveno.atp.acceptance.api.commands.CreateNewAccountCommand;
import org.reveno.atp.acceptance.api.commands.NewOrderCommand;
import org.reveno.atp.acceptance.api.events.AccountCreatedEvent;
import org.reveno.atp.acceptance.api.events.OrderCreatedEvent;
import org.reveno.atp.acceptance.api.transactions.AcceptOrder;
import org.reveno.atp.acceptance.api.transactions.CreateAccount;
import org.reveno.atp.acceptance.api.transactions.Credit;
import org.reveno.atp.acceptance.api.transactions.Debit;
import org.reveno.atp.acceptance.handlers.RollbackTransactions;
import org.reveno.atp.acceptance.handlers.Transactions;
import org.reveno.atp.acceptance.model.Account;
import org.reveno.atp.acceptance.model.Order.OrderType;
import org.reveno.atp.acceptance.views.AccountView;
import org.reveno.atp.acceptance.views.OrderView;
import org.reveno.atp.api.ChannelOptions;
import org.reveno.atp.api.Configuration.ModelType;
import org.reveno.atp.api.Configuration.MutableModelFailover;
import org.reveno.atp.api.Reveno;
import org.reveno.atp.api.commands.EmptyResult;
import org.reveno.atp.api.domain.Repository;
import org.reveno.atp.core.RevenoConfiguration;
import org.reveno.atp.core.api.serialization.RepositoryDataSerializer;
import org.reveno.atp.core.serialization.DefaultJavaSerializer;
import org.reveno.atp.core.serialization.ProtostuffSerializer;
import org.reveno.atp.core.snapshots.DefaultSnapshotter;
import org.reveno.atp.core.storage.FileSystemStorage;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class Tests extends RevenoBaseTest {
	
	@Test 
	public void testBasic() throws Exception {
		Reveno reveno = createEngine();
		reveno.startup();
		
		Waiter accountCreatedEvent = listenFor(reveno, AccountCreatedEvent.class);
		Waiter orderCreatedEvent = listenFor(reveno, OrderCreatedEvent.class);
		long accountId = sendCommandSync(reveno, new CreateNewAccountCommand("USD", 1000_000L));
		AccountView accountView = reveno.query().find(AccountView.class, accountId).get();
		
		Assert.assertTrue(accountCreatedEvent.isArrived());
		Assert.assertEquals(accountId, accountView.accountId);
		Assert.assertEquals("USD", accountView.currency);
		Assert.assertEquals(1000_000L, accountView.balance);
		Assert.assertEquals(0, accountView.orders().size());
		
		long orderId = sendCommandSync(reveno, new NewOrderCommand(accountId, null, "EUR/USD", 134000, 1000, OrderType.MARKET));
		OrderView orderView = reveno.query().find(OrderView.class, orderId).get();
		accountView = reveno.query().find(AccountView.class, accountId).get();
		
		Assert.assertTrue(orderCreatedEvent.isArrived());
		Assert.assertEquals(orderId, orderView.id);
		Assert.assertEquals(1, accountView.orders().size());
		
		reveno.shutdown();
	}
	
	@Test
	public void testAsyncHandlers() throws Exception {
		Reveno reveno = createEngine();
		reveno.startup();
		
		Waiter accountCreatedEvent = listenAsyncFor(reveno, AccountCreatedEvent.class, 1_000);
		sendCommandsBatch(reveno, new CreateNewAccountCommand("USD", 1000_000L), 1_000);
		Assert.assertTrue(accountCreatedEvent.isArrived());
		
		reveno.shutdown();
	}
	
	@Test
	public void testExceptionalEventHandler() throws Exception {
		Reveno reveno = createEngine();
		reveno.startup();
		
		Waiter w = listenFor(reveno, AccountCreatedEvent.class, 1_000, (c) -> {
			if (c == 500 || c == 600 || c == 601) {
				throw new RuntimeException();
			}
		});
		sendCommandsBatch(reveno, new CreateNewAccountCommand("USD", 1000_000L), 1_000);
		// it's just fine since on exception we still processing
		// but such events won't be committed
		Assert.assertTrue(w.isArrived());
		
		reveno.shutdown();
		
		reveno = createEngine();
		w = listenFor(reveno, AccountCreatedEvent.class, 4);
		reveno.startup();
		
		// after restart we expect that there will be 3 replayed
		// events - the count of exceptions
		Assert.assertFalse(w.isArrived());
		Assert.assertEquals(1, w.getCount());
		
		reveno.shutdown();
	}
	
	@Test
	public void testExceptionalAsyncEventHandler() throws Exception {
		TestRevenoEngine reveno = createEngine();
		reveno.events().asyncEventExecutors(10);
		reveno.startup();
		
		Waiter w = listenAsyncFor(reveno, AccountCreatedEvent.class, 1_000, (c) -> {
			if (c == 500 || c == 600 || c == 601) {
				throw new RuntimeException();
			}
		});
		sendCommandsBatch(reveno, new CreateNewAccountCommand("USD", 1000_000L), 1_000);
		Assert.assertTrue(w.isArrived(5));
		
		reveno.syncAll();
		reveno.shutdown();
		
		reveno = createEngine();
		w = listenFor(reveno, AccountCreatedEvent.class, 4);
		reveno.startup();
		
		Assert.assertFalse(w.isArrived());
		Assert.assertEquals(1, w.getCount());
		
		reveno.shutdown();
	}
	
	@Test
	public void testBatch() throws Exception {
		Reveno reveno = createEngine();
		Waiter accountsWaiter = listenFor(reveno, AccountCreatedEvent.class, 10_000);
		Waiter ordersWaiter = listenFor(reveno, OrderCreatedEvent.class, 10_000);
		reveno.startup();
		
		generateAndSendCommands(reveno, 10_000);
		
		Assert.assertEquals(10_000, reveno.query().select(AccountView.class).size());
		Assert.assertEquals(10_000, reveno.query().select(OrderView.class).size());
		
		Assert.assertTrue(accountsWaiter.isArrived());
		Assert.assertTrue(ordersWaiter.isArrived());
		
		reveno.shutdown();
	}
	
	@Test
	public void testReplay() throws Exception {
		testBasic();
		
		Reveno reveno = createEngine();
		Waiter accountCreatedEvent = listenFor(reveno, AccountCreatedEvent.class);
		Waiter orderCreatedEvent = listenFor(reveno, OrderCreatedEvent.class);
		reveno.startup();
		
		Assert.assertFalse(accountCreatedEvent.isArrived());
		Assert.assertFalse(orderCreatedEvent.isArrived());
		
		Assert.assertEquals(1, reveno.query().select(AccountView.class).size());
		Assert.assertEquals(1, reveno.query().select(OrderView.class).size());
		
		reveno.shutdown();
	}
	
	@Test
	public void testBatchReplay() throws Exception {
		testBatch();
		
		Reveno reveno = createEngine();
		Waiter accountsWaiter = listenFor(reveno, AccountCreatedEvent.class, 1);
		Waiter ordersWaiter = listenFor(reveno, OrderCreatedEvent.class, 1);
		reveno.startup();
		
		Assert.assertEquals(10_000, reveno.query().select(AccountView.class).size());
		Assert.assertEquals(10_000, reveno.query().select(OrderView.class).size());
		
		Assert.assertFalse(accountsWaiter.isArrived());
		Assert.assertFalse(ordersWaiter.isArrived());
		
		long accountId = sendCommandSync(reveno, new CreateNewAccountCommand("USD", 1000_000L));
		Assert.assertEquals(10_001, accountId);
		long orderId = sendCommandSync(reveno, new NewOrderCommand(accountId, null, "EUR/USD", 134000, 1000, OrderType.MARKET));
		Assert.assertEquals(10_001, orderId);
		
		reveno.shutdown();
	}
	
	@Test
	public void testShutdownSnapshotting() throws Exception {
		Reveno reveno = createEngine();
		reveno.config().snapshotting().snapshotAtShutdown(true);
		reveno.startup();
		
		generateAndSendCommands(reveno, 10_000);
		
		Assert.assertEquals(10_000, reveno.query().select(AccountView.class).size());
		Assert.assertEquals(10_000, reveno.query().select(OrderView.class).size());
		
		reveno.shutdown();
		
		Arrays.asList(tempDir.listFiles((dir, name) -> !(name.startsWith("snp")))).forEach(File::delete);
		
		reveno = createEngine();
		reveno.startup();
		
		Assert.assertEquals(10_000, reveno.query().select(AccountView.class).size());
		Assert.assertEquals(10_000, reveno.query().select(OrderView.class).size());
		
		reveno.shutdown();
	}
	
	@Test
	public void testSnapshottingEveryJavaSerializer() throws Exception {
		testSnapshottingEvery(new DefaultJavaSerializer());
	}
	
	@Test
	public void testSnapshottingEveryProtostuffSerializer() throws Exception {
		testSnapshottingEvery(new ProtostuffSerializer());
	}
	
	public void testSnapshottingEvery(RepositoryDataSerializer repoSerializer) throws Exception {
		Consumer<TestRevenoEngine> consumer = r -> {
			r.domain().resetSnapshotters();
			r.domain().snapshotWith(new DefaultSnapshotter(new FileSystemStorage(tempDir, new RevenoConfiguration.RevenoJournalingConfiguration()), repoSerializer))
				.andRestoreWithIt();
		};
		Reveno reveno = createEngine(consumer);
		reveno.config().snapshotting().snapshotAtShutdown(false);
		reveno.config().snapshotting().snapshotEvery(1002);
		reveno.startup();
		
		generateAndSendCommands(reveno, 10_005);
		
		Assert.assertEquals(10_005, reveno.query().select(AccountView.class).size());
		Assert.assertEquals(10_005, reveno.query().select(OrderView.class).size());
		
		reveno.shutdown();
		
		Assert.assertEquals(19, tempDir.listFiles((dir, name) -> name.startsWith("snp")).length);
		
		reveno = createEngine(consumer);
		reveno.startup();
		
		Assert.assertEquals(10_005, reveno.query().select(AccountView.class).size());
		Assert.assertEquals(10_005, reveno.query().select(OrderView.class).size());
		
		generateAndSendCommands(reveno, 3);
		
		reveno.shutdown();
		
		Arrays.asList(tempDir.listFiles((dir, name) -> name.startsWith("snp"))).forEach(File::delete);
		
		reveno = createEngine(consumer);
		Waiter accountCreatedEvent = listenFor(reveno, AccountCreatedEvent.class);
		Waiter orderCreatedEvent = listenFor(reveno, OrderCreatedEvent.class);
		reveno.startup();
		
		Assert.assertEquals(10_008, reveno.query().select(AccountView.class).size());
		Assert.assertEquals(10_008, reveno.query().select(OrderView.class).size());
		Assert.assertFalse(accountCreatedEvent.isArrived());
		Assert.assertFalse(orderCreatedEvent.isArrived());
		
		reveno.shutdown();
	}
	
	@Test
	public void testParallelRolling() throws Exception {
		final boolean[] stop =  { false };
		AtomicLong counter = new AtomicLong(0);
		ExecutorService transactionExecutor = Executors.newFixedThreadPool(10);
		TestRevenoEngine reveno = createEngine();
		reveno.startup();
		IntStream.range(0, 10).forEach(i -> transactionExecutor.submit(() -> {
			while (!stop[0]) {
				counter.incrementAndGet();
				try {
					sendCommandSync(reveno, new CreateNewAccountCommand("USD", 1000_000L));
				} catch (Exception e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			}
		}));
		
		IntStream.range(0, 20).forEach(i -> {
			Waiter w = new Waiter(1);
			reveno.roll(w::countDown);
			w.awaitSilent();
			sleep(200);
		});
		stop[0] = true;
		sleep(10);
		transactionExecutor.shutdown();
		
		reveno.shutdown();
		
		Reveno revenoRestarted = createEngine();
		Waiter accountCreatedEvent = listenFor(reveno, AccountCreatedEvent.class);
		revenoRestarted.startup();
		
		Assert.assertFalse(accountCreatedEvent.isArrived());
		Assert.assertEquals(counter.get(), reveno.query().select(AccountView.class).size());
		
		revenoRestarted.shutdown();
	}
	
	@Test
	public void testTransactionWithCompensatingActions() throws Exception {
		if (modelType != ModelType.MUTABLE) {
			return;
		}
		
		class TestTx {}
		class TestCmd {}
		
		Repository[] repo = new Repository[1];
		Consumer<TestRevenoEngine> consumer = r -> {
			r.config().modelType(ModelType.MUTABLE);
			r.config().mutableModelFailover(MutableModelFailover.COMPENSATING_ACTIONS);
			r.domain().transactionWithCompensatingAction(CreateAccount.class, Transactions::createAccount, RollbackTransactions::rollbackCreateAccount);
			r.domain().transactionWithCompensatingAction(AcceptOrder.class, Transactions::acceptOrder, RollbackTransactions::rollbackAcceptOrder);
			r.domain().transactionWithCompensatingAction(Credit.class, Transactions::credit, RollbackTransactions::rollbackCredit);
			r.domain().transactionWithCompensatingAction(Debit.class, Transactions::debit, RollbackTransactions::rollbackDebit);
			
			r.domain().command(TestCmd.class, (c,d) -> d.executeTransaction(new TestTx()));
			r.domain().transactionAction(TestTx.class, (a,b) -> { repo[0] = b.repository(); throw new RuntimeException(); });
		};
		Reveno reveno = createEngine(consumer);
		reveno.startup();
		
		long accountId = sendCommandSync(reveno, new CreateNewAccountCommand("USD", 1000));
		Future<EmptyResult> f = reveno.performCommands(Arrays.asList(new Credit(accountId, 15, 0), new Debit(accountId, 8),
				new NewOrderCommand(accountId, null, "EUR/USD", 134000, 1, OrderType.MARKET), new TestCmd()));
		
		Assert.assertFalse(f.get().isSuccess());
		Assert.assertEquals(RuntimeException.class, f.get().getException().getClass());
		Assert.assertEquals(1000, repo[0].get(Account.class, accountId).get().balance());
		Assert.assertEquals(1000, reveno.query().find(AccountView.class, accountId).get().balance);
		
		reveno.shutdown();
		
		reveno = createEngine(consumer);
		reveno.startup();
		
		Assert.assertEquals(1000, reveno.query().find(AccountView.class, accountId).get().balance);
		
		reveno.shutdown();
	}

	@Test
	public void testPreallocatedSingleVolume() throws Exception {
		Consumer<TestRevenoEngine> c = reveno -> {
			Assert.assertEquals(9, reveno.getJournalsStorage().getVolumes().length);
			Assert.assertEquals(1, reveno.getJournalsStorage().getLastStores().length);
		};
		testPreallocatedJournals(2_500_000, ChannelOptions.UNBUFFERED_IO, c);
		testPreallocatedJournals(2_500_000, ChannelOptions.BUFFERING_VM, c);
		testPreallocatedJournals(2_500_000, ChannelOptions.BUFFERING_MMAP_OS, c);
		testPreallocatedJournals(2_500_000, ChannelOptions.BUFFERING_OS, c);
		testPreallocatedJournals(2_500_000, ChannelOptions.UNBUFFERED_IO, r -> {}, true);
		testPreallocatedJournals(2_500_000, ChannelOptions.BUFFERING_VM, r -> {}, true);
		testPreallocatedJournals(2_500_000, ChannelOptions.BUFFERING_MMAP_OS, r -> {}, true);
		testPreallocatedJournals(2_500_000, ChannelOptions.BUFFERING_OS, r -> {}, true);
	}

	@Test
	public void testPreallocatedMultipleVolumes() throws Exception {
		Consumer<TestRevenoEngine> c = reveno -> {
			Assert.assertEquals(5, reveno.getJournalsStorage().getVolumes().length);
			Assert.assertEquals(5, reveno.getJournalsStorage().getLastStores().length);
		};
		testPreallocatedJournals(500_000, ChannelOptions.UNBUFFERED_IO, c);
		testPreallocatedJournals(500_000, ChannelOptions.BUFFERING_VM, c);
		testPreallocatedJournals(500_000, ChannelOptions.BUFFERING_MMAP_OS, c);
		testPreallocatedJournals(500_000, ChannelOptions.BUFFERING_OS, c);
		testPreallocatedJournals(500_000, ChannelOptions.UNBUFFERED_IO, r -> {}, true);
		testPreallocatedJournals(500_000, ChannelOptions.BUFFERING_VM, r -> {}, true);
		testPreallocatedJournals(500_000, ChannelOptions.BUFFERING_MMAP_OS, r -> {}, true);
		testPreallocatedJournals(500_000, ChannelOptions.BUFFERING_OS, r -> {}, true);
	}

	@Test
	public void testPreallocatedMultipleSmallVolumes() throws Exception {
		testPreallocatedJournals(50_000, ChannelOptions.UNBUFFERED_IO, r -> {});
		testPreallocatedJournals(50_000, ChannelOptions.BUFFERING_VM, r -> {});
		testPreallocatedJournals(50_000, ChannelOptions.BUFFERING_MMAP_OS, r -> {});
		testPreallocatedJournals(50_000, ChannelOptions.BUFFERING_OS, r -> {});
	}

	public void testPreallocatedJournals(long txSize, ChannelOptions channelOptions, Consumer<TestRevenoEngine> checks) throws Exception {
		testPreallocatedJournals(txSize, channelOptions, checks, false);
	}

	public void testPreallocatedJournals(long txSize, ChannelOptions channelOptions, Consumer<TestRevenoEngine> checks,
										 boolean javaSerializer) throws Exception {
		setUp();

		Consumer<TestRevenoEngine> consumer = r -> {
			r.config().journaling().minVolumes(1);
			r.config().journaling().volumes(10);
			r.config().journaling().preallocationSize(txSize, 500_000L);
			r.config().journaling().channelOptions(channelOptions);
			if (javaSerializer) {
				r.domain().serializeWith(Collections.singletonList(new DefaultJavaSerializer()));
			}
		};
		TestRevenoEngine reveno = createEngine(consumer);
		Waiter accountsWaiter = listenFor(reveno, AccountCreatedEvent.class, 10_000);
		Waiter ordersWaiter = listenFor(reveno, OrderCreatedEvent.class, 10_000);
		reveno.startup();

		generateAndSendCommands(reveno, 10_000);

		Assert.assertEquals(10_000, reveno.query().select(AccountView.class).size());
		Assert.assertEquals(10_000, reveno.query().select(OrderView.class).size());

		Assert.assertTrue(accountsWaiter.isArrived());
		Assert.assertTrue(ordersWaiter.isArrived());

		checks.accept(reveno);
		reveno.shutdown();

		reveno = createEngine(consumer);
		accountsWaiter = listenFor(reveno, AccountCreatedEvent.class, 1);
		ordersWaiter = listenFor(reveno, OrderCreatedEvent.class, 1);
		reveno.startup();

		Assert.assertEquals(10_000, reveno.query().select(AccountView.class).size());
		Assert.assertEquals(10_000, reveno.query().select(OrderView.class).size());

		Assert.assertFalse(accountsWaiter.isArrived());
		Assert.assertFalse(ordersWaiter.isArrived());

		long accountId = sendCommandSync(reveno, new CreateNewAccountCommand("USD", 1000_000L));
		Assert.assertEquals(10_001, accountId);
		long orderId = sendCommandSync(reveno, new NewOrderCommand(accountId, null, "EUR/USD", 134000, 1000, OrderType.MARKET));
		Assert.assertEquals(10_001, orderId);

		reveno.shutdown();

		tearDown();
	}
	
}
