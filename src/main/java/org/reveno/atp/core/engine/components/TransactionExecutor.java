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

import org.reveno.atp.api.commands.CommandContext;
import org.reveno.atp.api.domain.Repository;
import org.reveno.atp.core.engine.WorkflowContext;
import org.reveno.atp.core.engine.processor.ProcessorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionExecutor {

	public void executeCommands(ProcessorContext c, WorkflowContext services) {
		try {
			repository.underlyingRepository(services.repository());
			services.repository().begin();
			
			commandContext.withRepository(repository);
			
			
			commandContext.reset();
			
			services.repository().commit();
		} catch (Throwable t) {
			c.abort();
			log.error("executeCommands", t);
			services.repository().rollback();
			throw t;
		}
	}
	
	protected RecordingRepository repository = new RecordingRepository();
	protected InnerCommandContext commandContext = new InnerCommandContext();
	protected static final Logger log = LoggerFactory.getLogger(TransactionExecutor.class);
		
	protected static class InnerCommandContext implements CommandContext {
		private Repository repository;
		public Repository repository() {
			return repository;
		}
		
		private List<Object> transactions = new ArrayList<>();
		public CommandContext executeTransaction(Object transactionParam) {
			transactions.add(transactionParam);
			return this;
		}
		
		public InnerCommandContext reset() {
			repository = null;
			transactions.clear();
			return this;
		}
		
		public InnerCommandContext withRepository(Repository repository) {
			this.repository = repository;
			return this;
		}
		
	};
	
}
