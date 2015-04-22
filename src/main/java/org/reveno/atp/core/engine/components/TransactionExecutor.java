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

package org.reveno.atp.core.engine.components;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.reveno.atp.api.commands.CommandContext;
import org.reveno.atp.api.domain.Repository;
import org.reveno.atp.api.domain.WriteableRepository;
import org.reveno.atp.api.transaction.EventBus;
import org.reveno.atp.api.transaction.TransactionContext;
import org.reveno.atp.core.engine.WorkflowContext;
import org.reveno.atp.core.engine.processor.ProcessorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionExecutor {

	public void executeCommands(ProcessorContext c, WorkflowContext services, Supplier<Long> nextTransactionId) {
		if (!c.isRestore())
			c.transactionId(nextTransactionId.get());
		try {
			c.eventBus().currentTransactionId(c.transactionId()).underlyingEventBus(c.defaultEventBus());
			repository.underlying(services.repository()).map(c.getMarkedRecords());
			transactionContext.withContext(c).withRepository(repository);
			commandContext.withRepository(repository);
			
			services.repository().begin();
			
			if (c.isRestore()) {
				commandContext.transactions.addAll(c.getTransactions());
				executeTransactions(services);
			} else {
				c.getCommands().forEach(cmd -> {
					Object result = services.commandsManager().execute(cmd, commandContext);
					executeTransactions(services);
					c.addTransactions(commandContext.transactions);
					if (c.hasResult())
						c.commandResult(result);
				});
			}
			
			services.repository().commit();
		} catch (Throwable t) {
			c.abort(t);
			log.error("executeCommands", t);
			services.repository().rollback();
		}
	}

	protected void executeTransactions(WorkflowContext services) {
		services.transactionsManager().execute(commandContext.transactions, transactionContext);
	}
	
	protected RecordingRepository repository = new RecordingRepository();
	protected InnerCommandContext commandContext = new InnerCommandContext();
	protected InnerTransactionContext transactionContext = new InnerTransactionContext();
	protected static final Logger log = LoggerFactory.getLogger(TransactionExecutor.class);
		
	protected static class InnerCommandContext implements CommandContext {
		public Repository repository;
		public List<Object> transactions = new ArrayList<>();
		
		@Override
		public Repository repository() {
			return repository;
		}
		
		@Override
		public CommandContext executeTransaction(Object transactionParam) {
			transactions.add(transactionParam);
			return this;
		}
		
		public InnerCommandContext withRepository(Repository repository) {
			this.repository = repository;
			transactions.clear();
			return this;
		}
	};
	
	protected static class InnerTransactionContext implements TransactionContext {
		public EventBus eventBus;
		public WriteableRepository repository;

		@Override
		public EventBus eventBus() {
			return eventBus;
		}

		@Override
		public WriteableRepository repository() {
			return repository;
		}
		
		public InnerTransactionContext withContext(ProcessorContext context) {
			this.eventBus = context.eventBus();
			return this;
		}
		
		public InnerTransactionContext withRepository(WriteableRepository repository) {
			this.repository = repository;
			return this;
		}
	}
	
}
