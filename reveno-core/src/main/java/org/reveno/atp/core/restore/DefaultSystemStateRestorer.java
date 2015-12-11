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

package org.reveno.atp.core.restore;

import org.reveno.atp.core.EngineEventsContext;
import org.reveno.atp.core.EngineWorkflowContext;
import org.reveno.atp.core.api.*;
import org.reveno.atp.core.api.InputProcessor.JournalType;
import org.reveno.atp.core.api.storage.JournalsStorage;
import org.reveno.atp.core.data.DefaultInputProcessor;
import org.reveno.atp.core.engine.WorkflowEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultSystemStateRestorer implements SystemStateRestorer {

	/**
	 * Reads all journals and restores all previous system mode state from them
	 * into the given repository. Also, re-publish all events that were not sent
	 * by the reason of failure, abortion, etc.
	 *
	 * @param repository into which latest state of model will be loaded
	 * @return information about last system state, such as last transactionId, etc.
	 */
	@Override
	public SystemState restore(TxRepository repository) {
		workflowContext.repository(repository);
		final long snapshotTransactionId = repository.getO(SystemInfo.class, 0L).orElse(new SystemInfo(0L)).lastTransactionId;
		final long[] transactionId = { snapshotTransactionId };
		try (InputProcessor processor = new DefaultInputProcessor(journalStorage)) {
			processor.process(b -> {
				EventsCommitInfo e = eventsContext.serializer().deserialize(eventsContext.eventsCommitBuilder(), b);
				eventBus.processNextEvent(e);
			}, JournalType.EVENTS);
			processor.process(b -> {
				TransactionCommitInfo tx = workflowContext.serializer().deserialize(workflowContext.transactionCommitBuilder(), b);
				if (tx.transactionId() > transactionId[0] || tx.transactionId() == snapshotTransactionId) {
					transactionId[0] = tx.transactionId();
					workflowEngine.getPipe().executeRestore(eventBus, tx);
				} else if (LOG.isDebugEnabled()) {
					LOG.debug("Transaction ID {} less than last Transaction ID {}", tx.transactionId(), transactionId[0]);
				}
			}, JournalType.TRANSACTIONS);
		} catch (Throwable t) {
			LOG.error("restore", t);
			throw new RuntimeException(t);
		}
		workflowEngine.getPipe().sync();
		workflowContext.eventPublisher().getPipe().sync();
		return new SystemState(transactionId[0]);
	}
	
	public DefaultSystemStateRestorer(JournalsStorage journalStorage,
			EngineWorkflowContext workflowContext,
			EngineEventsContext eventsContext,
			WorkflowEngine workflowEngine) {
		this.journalStorage = journalStorage;
		this.workflowContext = workflowContext;
		this.eventsContext = eventsContext;
		this.workflowEngine = workflowEngine;
	}

	protected final RestorerEventBus eventBus = new RestorerEventBus();
	protected final JournalsStorage journalStorage;
	protected final EngineWorkflowContext workflowContext;
	protected final EngineEventsContext eventsContext;
	protected final WorkflowEngine workflowEngine;
	
	protected static final Logger LOG = LoggerFactory.getLogger(DefaultSystemStateRestorer.class);
}
