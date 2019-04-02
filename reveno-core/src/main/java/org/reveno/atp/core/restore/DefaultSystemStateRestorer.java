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
	protected static final Logger LOG = LoggerFactory.getLogger(DefaultSystemStateRestorer.class);
	protected final RestorerEventBus eventBus = new RestorerEventBus();
	protected final JournalsStorage journalStorage;
	protected final EngineWorkflowContext workflowContext;
	protected final EngineEventsContext eventsContext;
	protected final WorkflowEngine workflowEngine;

	public DefaultSystemStateRestorer(JournalsStorage journalStorage,
									  EngineWorkflowContext workflowContext,
									  EngineEventsContext eventsContext,
									  WorkflowEngine workflowEngine) {
		this.journalStorage = journalStorage;
		this.workflowContext = workflowContext;
		this.eventsContext = eventsContext;
		this.workflowEngine = workflowEngine;
	}

	/**
	 * Reads all journals and restores all previous system mode state from them
	 * into the given repository. Also, re-publish all events that were not sent
	 * by the reason of failure, abortion, etc.
	 *
	 * @param repository into which latest state of model will be loaded
	 * @return information about last system state, such as last transactionId, etc.
	 */
	@Override
	public SystemState restore(long fromVersion, TxRepository repository) {
		workflowContext.repository(repository);
		final long snapshotTransactionId = repository.getO(SystemInfo.class, 0L).orElse(new SystemInfo(0L)).lastTransactionId;
		final long[] transactionId = { snapshotTransactionId };
		try (InputProcessor processor = new DefaultInputProcessor(journalStorage)) {
			processor.process(fromVersion, b -> {
				EventsCommitInfo e = eventsContext.serializer().deserialize(eventsContext.eventsCommitBuilder(), b);
				eventBus.processNextEvent(e);
			}, JournalType.EVENTS);
			processor.process(fromVersion, b -> {
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
}
