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
	@Override
	public RevenoConfiguration configuration() {
		return configuration;
	}
	public EngineWorkflowContext configuration(RevenoConfiguration configuration) {
		this.configuration = configuration;
		return this;
	}

	private FailoverManager failoverManager;
	@Override
	public FailoverManager failoverManager() {
		return failoverManager;
	}
	public EngineWorkflowContext failoverManager(FailoverManager failoverManager) {
		this.failoverManager = failoverManager;
		return this;
	}

	private IdGenerator idGenerator;
	@Override
	public IdGenerator idGenerator() {
		return idGenerator;
	}
	public EngineWorkflowContext idGenerator(IdGenerator idGenerator) {
		this.idGenerator = idGenerator;
		return this;
	}
	
	private SerializersChain serializer;
	@Override
	public SerializersChain serializer() {
		return serializer;
	}
	public EngineWorkflowContext serializers(SerializersChain serializer) {
		this.serializer = serializer;
		return this;
	}

	private TxRepository repository;
	@Override
	public TxRepository repository() {
		return repository;
	}
	public EngineWorkflowContext repository(TxRepository repository) {
		this.repository = repository;
		return this;
	}

	private ViewsProcessor viewsProcessor;
	@Override
	public ViewsProcessor viewsProcessor() {
		return viewsProcessor;
	}
	public EngineWorkflowContext viewsProcessor(ViewsProcessor viewsProcessor) {
		this.viewsProcessor = viewsProcessor;
		return this;
	}

	private TransactionsManager transactionsManager;
	@Override
	public TransactionsManager transactionsManager() {
		return transactionsManager;
	}
	public EngineWorkflowContext transactionsManager(TransactionsManager transactionsManager) {
		this.transactionsManager = transactionsManager;
		return this;
	}
	
	private CommandsManager commandsManager;
	@Override
	public CommandsManager commandsManager() {
		return commandsManager;
	}
	public EngineWorkflowContext commandsManager(CommandsManager commandsManager) {
		this.commandsManager = commandsManager;
		return this;
	}

	private EventPublisher eventPublisher;
	@Override
	public EventPublisher eventPublisher() {
		return eventPublisher;
	}
	public EngineWorkflowContext eventPublisher(EventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
		return this;
	}

	private Builder transactionCommitBuilder;
	@Override
	public Builder transactionCommitBuilder() {
		return transactionCommitBuilder;
	}
	public EngineWorkflowContext transactionCommitBuilder(Builder transactionCommitBuilder) {
		this.transactionCommitBuilder = transactionCommitBuilder;
		return this;
	}

	private Journaler transactionJournaler;
	@Override
	public Journaler transactionJournaler() {
		return transactionJournaler;
	}
	public EngineWorkflowContext transactionJournaler(Journaler transactionJournaler) {
		this.transactionJournaler = transactionJournaler;
		return this;
	}
	
	private JournalsManager journalsManager;
	@Override
	public JournalsManager journalsManager() {
		return journalsManager;
	}
	public EngineWorkflowContext journalsManager(JournalsManager journalsManager) {
		this.journalsManager = journalsManager;
		return this;
	}
	
	private SnapshottersManager snapshotsManager;
	@Override
	public SnapshottersManager snapshotsManager() {
		return snapshotsManager;
	}
	public EngineWorkflowContext snapshotsManager(SnapshottersManager snapshotsManager) {
		this.snapshotsManager = snapshotsManager;
		return this;
	}
	
	private InterceptorCollection interceptorCollection;
	@Override
	public InterceptorCollection interceptorCollection() {
		return interceptorCollection;
	}
	public EngineWorkflowContext interceptorCollection(InterceptorCollection interceptorCollection) {
		this.interceptorCollection = interceptorCollection;
		return this;
	}

}
