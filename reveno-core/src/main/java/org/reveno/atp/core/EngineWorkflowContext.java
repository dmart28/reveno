package org.reveno.atp.core;

import org.reveno.atp.core.api.*;
import org.reveno.atp.core.api.TransactionCommitInfo.Builder;
import org.reveno.atp.core.engine.WorkflowContext;
import org.reveno.atp.core.engine.components.CommandsManager;
import org.reveno.atp.core.engine.components.SerializersChain;
import org.reveno.atp.core.engine.components.TransactionsManager;
import org.reveno.atp.core.events.EventPublisher;
import org.reveno.atp.core.snapshots.SnapshottersManager;
import org.reveno.atp.core.views.ViewsProcessor;

public class EngineWorkflowContext implements WorkflowContext {
	private RevenoConfiguration configuration;
	private ClassLoader classLoader;
	private IdGenerator idGenerator;
	private SerializersChain serializer;
	private TxRepository repository;
	private ViewsProcessor viewsProcessor;
	private TransactionsManager transactionsManager;
	private CommandsManager commandsManager;
	private EventPublisher eventPublisher;
	private Builder transactionCommitBuilder;
	private Journaler transactionJournaler;
	private JournalsManager journalsManager;
	private SnapshottersManager snapshotsManager;
	private FailoverManager failoverManager;
	private InterceptorCollection interceptorCollection;

	@Override
	public RevenoConfiguration configuration() {
		return configuration;
	}

	public EngineWorkflowContext configuration(RevenoConfiguration configuration) {
		this.configuration = configuration;
		return this;
	}

	@Override
	public ClassLoader classLoader() {
		return classLoader;
	}

	public EngineWorkflowContext classLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
		return this;
	}

	@Override
	public IdGenerator idGenerator() {
		return idGenerator;
	}

	public EngineWorkflowContext idGenerator(IdGenerator idGenerator) {
		this.idGenerator = idGenerator;
		return this;
	}

	@Override
	public SerializersChain serializer() {
		return serializer;
	}

	public EngineWorkflowContext serializers(SerializersChain serializer) {
		this.serializer = serializer;
		return this;
	}

	@Override
	public TxRepository repository() {
		return repository;
	}

	public EngineWorkflowContext repository(TxRepository repository) {
		this.repository = repository;
		return this;
	}

	@Override
	public ViewsProcessor viewsProcessor() {
		return viewsProcessor;
	}

	public EngineWorkflowContext viewsProcessor(ViewsProcessor viewsProcessor) {
		this.viewsProcessor = viewsProcessor;
		return this;
	}

	@Override
	public TransactionsManager transactionsManager() {
		return transactionsManager;
	}

	public EngineWorkflowContext transactionsManager(TransactionsManager transactionsManager) {
		this.transactionsManager = transactionsManager;
		return this;
	}

	@Override
	public CommandsManager commandsManager() {
		return commandsManager;
	}

	public EngineWorkflowContext commandsManager(CommandsManager commandsManager) {
		this.commandsManager = commandsManager;
		return this;
	}

	@Override
	public EventPublisher eventPublisher() {
		return eventPublisher;
	}

	public EngineWorkflowContext eventPublisher(EventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
		return this;
	}

	@Override
	public Builder transactionCommitBuilder() {
		return transactionCommitBuilder;
	}

	public EngineWorkflowContext transactionCommitBuilder(Builder transactionCommitBuilder) {
		this.transactionCommitBuilder = transactionCommitBuilder;
		return this;
	}

	@Override
	public Journaler transactionJournaler() {
		return transactionJournaler;
	}

	public EngineWorkflowContext transactionJournaler(Journaler transactionJournaler) {
		this.transactionJournaler = transactionJournaler;
		return this;
	}

	@Override
	public JournalsManager journalsManager() {
		return journalsManager;
	}

	public EngineWorkflowContext journalsManager(JournalsManager journalsManager) {
		this.journalsManager = journalsManager;
		return this;
	}

	@Override
	public SnapshottersManager snapshotsManager() {
		return snapshotsManager;
	}

	public EngineWorkflowContext snapshotsManager(SnapshottersManager snapshotsManager) {
		this.snapshotsManager = snapshotsManager;
		return this;
	}

	@Override
	public FailoverManager failoverManager() {
		return failoverManager;
	}

	public EngineWorkflowContext failoverManager(FailoverManager failoverManager) {
		this.failoverManager = failoverManager;
		return this;
	}

	@Override
	public InterceptorCollection interceptorCollection() {
		return interceptorCollection;
	}

	public EngineWorkflowContext interceptorCollection(InterceptorCollection interceptorCollection) {
		this.interceptorCollection = interceptorCollection;
		return this;
	}

}
