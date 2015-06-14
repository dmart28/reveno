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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.reveno.atp.acceptance.api.commands.CreateNewAccountCommand;
import org.reveno.atp.acceptance.api.commands.NewOrderCommand;
import org.reveno.atp.acceptance.api.transactions.AcceptOrder;
import org.reveno.atp.acceptance.api.transactions.CreateAccount;
import org.reveno.atp.acceptance.api.transactions.Credit;
import org.reveno.atp.acceptance.api.transactions.Debit;
import org.reveno.atp.acceptance.handlers.Commands;
import org.reveno.atp.acceptance.handlers.Transactions;
import org.reveno.atp.acceptance.model.Account;
import org.reveno.atp.acceptance.model.Order;
import org.reveno.atp.acceptance.model.Order.OrderType;
import org.reveno.atp.acceptance.model.immutable.ImmutableAccount;
import org.reveno.atp.acceptance.model.immutable.ImmutableOrder;
import org.reveno.atp.acceptance.model.mutable.MutableAccount;
import org.reveno.atp.acceptance.model.mutable.MutableOrder;
import org.reveno.atp.acceptance.views.AccountView;
import org.reveno.atp.acceptance.views.OrderView;
import org.reveno.atp.api.Configuration.CpuConsumption;
import org.reveno.atp.api.Configuration.ModelType;
import org.reveno.atp.api.Reveno;
import org.reveno.atp.api.commands.Result;
import org.reveno.atp.api.domain.WriteableRepository;
import org.reveno.atp.api.transaction.TransactionInterceptor;
import org.reveno.atp.api.transaction.TransactionStage;
import org.reveno.atp.test.utils.FileUtils;

import com.google.common.io.Files;

@RunWith(Parameterized.class)
public class RevenoBaseTest {
	
	@Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                 { ModelType.IMMUTABLE }, { ModelType.MUTABLE }  
           });
    }
    
    @Parameter
    public ModelType modelType;
	
	protected File tempDir;
	
	@Before
	public void setUp() {
		// need to tell protostuff to treat empty collections not as null
		System.setProperty("protostuff.runtime.collection_schema_on_repeated_fields", "true");
		tempDir = Files.createTempDir();
	}
	
	@After
	public void tearDown() throws IOException {
		FileUtils.delete(tempDir);
	}
	
	@Test
	public void mockTest() {
		// gradle requires such hack though
	}
	
	protected TestRevenoEngine createEngine() {
		return createEngine((r) -> {});
	}

	protected TestRevenoEngine createEngine(Consumer<Reveno> consumer) {
		TestRevenoEngine reveno = new TestRevenoEngine(tempDir);
		
		reveno.config().cpuConsumption(CpuConsumption.PHASED);
		reveno.config().modelType(modelType);
		
		reveno.domain().command(CreateNewAccountCommand.class, Long.class, Commands::createAccount);
		reveno.domain().command(NewOrderCommand.class, Long.class, Commands::newOrder);
		reveno.domain().transactionAction(CreateAccount.class, Transactions::createAccount);
		reveno.domain().transactionAction(AcceptOrder.class, Transactions::acceptOrder);
		reveno.domain().transactionAction(Credit.class, Transactions::credit);
		reveno.domain().transactionAction(Debit.class, Transactions::debit);
		
		reveno.domain().viewMapper(Account.class, AccountView.class, (e, ov, r) -> {
			return new AccountView(e.id(), e.currency(), e.balance(), e.orders(), reveno.query());
		});
		reveno.domain().viewMapper(Order.class, OrderView.class, (e, ov, r) -> {
			return new OrderView(e.id(), e.accountId(), e.size(), e.price(), e.time(), e.positionId(),
					e.symbol(), e.orderStatus(), e.orderType(), reveno.query());
		});
		
		txPerSecondMeter(reveno);
		
		consumer.accept(reveno);
		
		if (modelType == ModelType.IMMUTABLE) {
			Transactions.accountFactory = ImmutableAccount.FACTORY;
			Transactions.orderFactory = ImmutableOrder.FACTORY;
		} else {
			Transactions.accountFactory = MutableAccount.FACTORY;
			Transactions.orderFactory = MutableOrder.FACTORY;
		}
		
		return reveno;
	}

	protected void txPerSecondMeter(TestRevenoEngine reveno) {
		reveno.interceptors().add(TransactionStage.TRANSACTION, new TransactionInterceptor() {
			protected long start = -1L;
			@Override
			public void intercept(long transactionId, WriteableRepository repository,
					TransactionStage stage) {
				if (start == -1 || transactionId == 1) start = System.currentTimeMillis();
				if (transactionId % 1000 == 0) {
					System.err.println(String.format("%s tx per second!", (
							Math.round((double)1000 * 1000) / (System.currentTimeMillis() - start))));
					start = System.currentTimeMillis();
				}
			}
		});
	}
	
	protected void generateAndSendCommands(Reveno reveno, int count)
			throws InterruptedException, ExecutionException {
		sendCommandsBatch(reveno, new CreateNewAccountCommand("USD", 1000_000L), count);
		
		List<NewOrderCommand> commands = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			commands.add(new NewOrderCommand((long)(count*Math.random()) + 1, Optional.empty(), "EUR/USD",
					134000, (long)(1000*Math.random()), OrderType.MARKET));
		}
		sendCommandsBatch(reveno, commands);
	}
	
	protected File findFirstFile(String prefix) {
		return tempDir.listFiles((dir, name) -> name.startsWith(prefix))[0];
	}
	
	@SuppressWarnings("unchecked")
	protected <T> T sendCommandSync(Reveno reveno, Object command) throws InterruptedException, ExecutionException {
		Result<T> result = (Result<T>) reveno.executeCommand(command).get();
		if (!result.isSuccess())
			throw new RuntimeException(result.getException());
		return result.getResult();
	}
	
	protected void sendCommandsBatch(Reveno reveno, Object command, int n) throws InterruptedException, ExecutionException {
		for (int i = 0; i < n - 1; i++) {
			reveno.executeCommand(command);
		}
		sendCommandSync(reveno, command);
	}
	
	protected void sendCommandsBatch(Reveno reveno, List<? extends Object> commands) throws InterruptedException, ExecutionException {
		for (int i = 0; i < commands.size() - 1; i++) {
			reveno.executeCommand(commands.get(i));
		}
		sendCommandSync(reveno, commands.get(commands.size() - 1));
	}
	
	protected <T> Waiter listenFor(Reveno reveno, Class<T> event) {
		Waiter waiter = new Waiter(1);
		reveno.events().eventHandler(event, (e,m) -> waiter.countDown());
		return waiter;
	}
	
	protected <T> Waiter listenAsyncFor(Reveno reveno, Class<T> event, int n) {
		return listenAsyncFor(reveno, event, n, (o) -> {});
	}
	
	protected <T> Waiter listenAsyncFor(Reveno reveno, Class<T> event, int n, Consumer<Long> c) {
		Waiter waiter = new Waiter(n);
		reveno.events().asyncEventHandler(event, (e,m) -> {
			waiter.countDown();
			c.accept(waiter.getCount());
		});
		return waiter;
	}
	
	protected <T> Waiter listenFor(Reveno reveno, Class<T> event, int n) {
		return listenFor(reveno, event, n, (a) -> {});
	}
	
	protected <T> Waiter listenFor(Reveno reveno, Class<T> event, int n, Consumer<Long> c) {
		Waiter waiter = new Waiter(n);
		reveno.events().eventHandler(event, (e,m) -> {
			waiter.countDown();
			c.accept(waiter.getCount());
		});
		return waiter;
	}
	
	protected void sleep(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static class Waiter extends CountDownLatch {

		public Waiter(int count) {
			super(count);
		}
		
		public boolean isArrived() throws InterruptedException {
			return await(500, TimeUnit.MILLISECONDS);
		}
		
		public void awaitSilent() {
			try {
				await(500, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
			}
		}
		
	}
	
}
