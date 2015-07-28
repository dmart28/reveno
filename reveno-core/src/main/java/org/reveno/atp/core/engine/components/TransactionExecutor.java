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

import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.reveno.atp.api.Configuration.ModelType;
import org.reveno.atp.api.Configuration.MutableModelFailover;
import org.reveno.atp.api.commands.CommandContext;
import org.reveno.atp.api.domain.Repository;
import org.reveno.atp.api.domain.WriteableRepository;
import org.reveno.atp.api.transaction.EventBus;
import org.reveno.atp.api.transaction.TransactionContext;
import org.reveno.atp.core.api.IdGenerator;
import org.reveno.atp.core.api.SystemInfo;
import org.reveno.atp.core.disruptor.ProcessorContext;
import org.reveno.atp.core.engine.WorkflowContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionExecutor {

	public void executeCommands(ProcessorContext c, WorkflowContext services) {
		ListIterator<Object> cmdIterator = c.getCommands().listIterator();
		ListIterator<Object> transactionIterator = c.getTransactions().listIterator();
		try {
			c.eventBus().currentTransactionId(c.transactionId()).underlyingEventBus(c.defaultEventBus());
			repository.underlying(services.repository()).map(c.getMarkedRecords());
			transactionContext.withContext(c).withRepository(repository).reset();
			commandContext.withRepository(repository).withTransactionsHolder(c.getTransactions()).idGenerator(services.idGenerator());
			
			begin(services);
			
			if (c.isRestore()) {
				executeTransactions(services, transactionIterator);
			} else {
				int index = 0;
				while (cmdIterator.hasNext()) {
					Object result = services.commandsManager().execute(cmdIterator.next(), commandContext);
					transactionIterator = c.getTransactions().listIterator(index);
					executeTransactions(services, transactionIterator);
					if (c.hasResult())
						c.commandResult(result);
					index = c.getTransactions().size();
				}
			}
			
			commit(services);
			setSystemInfo(services, c.transactionId());
		} catch (Throwable t) {
			c.abort(t);
			log.error("executeCommands", t);
			rollback(services, c);
		}
	}

	protected void rollback(WorkflowContext services, ProcessorContext c) {
		if (services.configuration().modelType() == ModelType.MUTABLE && 
				services.configuration().mutableModelFailover() == MutableModelFailover.SNAPSHOTS) {
			services.repository().rollback();
		} else {
			rollbackTransactions(services, c.getTransactions().listIterator(c.getTransactions().size() - 1));
		}
	}

	protected void commit(WorkflowContext services) {
		if (services.configuration().modelType() == ModelType.MUTABLE && 
				services.configuration().mutableModelFailover() == MutableModelFailover.SNAPSHOTS)
			services.repository().commit();
	}

	protected void begin(WorkflowContext services) {
		if (services.configuration().modelType() == ModelType.MUTABLE && 
				services.configuration().mutableModelFailover() == MutableModelFailover.SNAPSHOTS)
			services.repository().begin();
	}

	protected void executeTransactions(WorkflowContext services, ListIterator<Object> i) {
		while (i.hasNext())
			services.transactionsManager().execute(i.next(), transactionContext);
	}
	
	protected void rollbackTransactions(WorkflowContext services, ListIterator<Object> transactionIterator) {
		forEachPrev(transactionIterator, t -> services.transactionsManager().rollback(t, transactionContext));
	}
	
	protected void setSystemInfo(WorkflowContext services, long transactionId) {
		Optional<SystemInfo> si = services.repository().get(SystemInfo.class, 0L);
		if (si.isPresent())
			si.get().lastTransactionId = transactionId;
		else
			services.repository().store(0L, SystemInfo.class, new SystemInfo(transactionId));
	}
	
	protected void forEachPrev(ListIterator<Object> i, Consumer<Object> c) {
		while (i.hasPrevious()) {
			c.accept(i.previous());
		}
	}
	
	protected RecordingRepository repository = new RecordingRepository();
	protected InnerCommandContext commandContext = new InnerCommandContext();
	protected InnerTransactionContext transactionContext = new InnerTransactionContext();
	protected static final Logger log = LoggerFactory.getLogger(TransactionExecutor.class);
		
	protected static class InnerCommandContext implements CommandContext {
		public Repository repository;
		protected List<Object> transactions;
		public IdGenerator idGenerator;
		
		@Override
		public Repository repository() {
			return repository;
		}
		
		@Override
		public long id(Class<?> type) {
			return idGenerator.next(type);
		}
		
		@Override
		public CommandContext executeTransaction(Object transactionParam) {
			transactions.add(transactionParam);
			return this;
		}
		
		public InnerCommandContext withRepository(Repository repository) {
			this.repository = repository;
			return this;
		}
		
		public InnerCommandContext withTransactionsHolder(List<Object> transactions) {
			this.transactions = transactions;
			return this;
		}
		
		public InnerCommandContext idGenerator(IdGenerator idGenerator) {
			this.idGenerator = idGenerator;
			this.idGenerator.context(this);
			return this;
		}
	};
	
	protected static class InnerTransactionContext implements TransactionContext {
		public EventBus eventBus;
		public WriteableRepository repository;
		protected Map<Object, Object> map = new HashMap<>();

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
		
		public void reset() {
			map.clear();
		}

		@Override
		public Map<Object, Object> data() {
			return map;
		}
	}
	
}
