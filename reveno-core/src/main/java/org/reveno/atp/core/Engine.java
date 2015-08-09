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

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.reveno.atp.api.ClusterManager;
import org.reveno.atp.api.Configuration;
import org.reveno.atp.api.Configuration.CpuConsumption;
import org.reveno.atp.api.EventsManager;
import org.reveno.atp.api.RepositorySnapshotter;
import org.reveno.atp.api.Reveno;
import org.reveno.atp.api.RevenoManager;
import org.reveno.atp.api.commands.CommandContext;
import org.reveno.atp.api.commands.EmptyResult;
import org.reveno.atp.api.commands.Result;
import org.reveno.atp.api.dynamic.AbstractDynamicTransaction;
import org.reveno.atp.api.dynamic.DirectTransactionBuilder;
import org.reveno.atp.api.dynamic.DynamicCommand;
import org.reveno.atp.api.domain.RepositoryData;
import org.reveno.atp.api.domain.WriteableRepository;
import org.reveno.atp.api.query.QueryManager;
import org.reveno.atp.api.query.ViewsMapper;
import org.reveno.atp.api.transaction.TransactionContext;
import org.reveno.atp.api.transaction.TransactionInterceptor;
import org.reveno.atp.api.transaction.TransactionStage;
import org.reveno.atp.core.api.Destroyable;
import org.reveno.atp.core.api.EventsCommitInfo;
import org.reveno.atp.core.api.InterceptorCollection;
import org.reveno.atp.core.api.Journaler;
import org.reveno.atp.core.api.SystemStateRestorer;
import org.reveno.atp.core.api.TransactionCommitInfo;
import org.reveno.atp.core.api.TxRepository;
import org.reveno.atp.core.api.TxRepositoryFactory;
import org.reveno.atp.core.api.serialization.EventsInfoSerializer;
import org.reveno.atp.core.api.serialization.RepositoryDataSerializer;
import org.reveno.atp.core.api.serialization.TransactionInfoSerializer;
import org.reveno.atp.core.api.storage.FoldersStorage;
import org.reveno.atp.core.api.storage.JournalsStorage;
import org.reveno.atp.core.api.storage.JournalsStorage.JournalStore;
import org.reveno.atp.core.api.storage.SnapshotStorage;
import org.reveno.atp.core.data.DefaultJournaler;
import org.reveno.atp.core.disruptor.DisruptorEventPipeProcessor;
import org.reveno.atp.core.disruptor.DisruptorTransactionPipeProcessor;
import org.reveno.atp.core.disruptor.ProcessorContext;
import org.reveno.atp.core.engine.WorkflowEngine;
import org.reveno.atp.core.engine.components.CommandsManager;
import org.reveno.atp.core.engine.components.DefaultIdGenerator;
import org.reveno.atp.core.engine.components.DefaultIdGenerator.NextIdTransaction;
import org.reveno.atp.core.engine.components.SerializersChain;
import org.reveno.atp.core.engine.components.SnapshottingInterceptor;
import org.reveno.atp.core.engine.components.TransactionsManager;
import org.reveno.atp.core.engine.processor.PipeProcessor;
import org.reveno.atp.core.engine.processor.TransactionPipeProcessor;
import org.reveno.atp.core.events.Event;
import org.reveno.atp.core.events.EventHandlersManager;
import org.reveno.atp.core.events.EventPublisher;
import org.reveno.atp.core.impl.EventsCommitInfoImpl;
import org.reveno.atp.core.impl.TransactionCommitInfoImpl;
import org.reveno.atp.core.repository.HashMapRepository;
import org.reveno.atp.core.repository.MutableModelRepository;
import org.reveno.atp.core.repository.SnapshotBasedModelRepository;
import org.reveno.atp.core.restore.DefaultSystemStateRestorer;
import org.reveno.atp.core.serialization.DefaultJavaSerializer;
import org.reveno.atp.core.serialization.SimpleEventsSerializer;
import org.reveno.atp.core.snapshots.SnapshottersManager;
import org.reveno.atp.core.storage.FileSystemStorage;
import org.reveno.atp.core.views.ViewsDefaultStorage;
import org.reveno.atp.core.views.ViewsManager;
import org.reveno.atp.core.views.ViewsProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Engine implements Reveno {
	
	public Engine(FoldersStorage foldersStorage, JournalsStorage journalsStorage,
			SnapshotStorage snapshotStorage, ClassLoader classLoader) {
		this.classLoader = classLoader;
		this.foldersStorage = foldersStorage;
		this.journalsStorage = journalsStorage;
		this.snapshotStorage = snapshotStorage;
		this.snapshotsManager = new SnapshottersManager(snapshotStorage, repositorySerializer);
		serializer = new SerializersChain(classLoader);
	}
	
	public Engine(File baseDir) {
		this(baseDir, Engine.class.getClassLoader());
	}
	
	public Engine(File baseDir, ClassLoader classLoader) {
		FileSystemStorage storage = new FileSystemStorage(baseDir);
		this.classLoader = classLoader;
		this.foldersStorage = storage;
		this.journalsStorage = storage;
		this.snapshotStorage = storage;
		this.snapshotsManager = new SnapshottersManager(snapshotStorage, repositorySerializer);
		serializer = new SerializersChain(classLoader);
	}
	
	public Engine(String baseDir) {
		this(new File(baseDir), Engine.class.getClassLoader());
	}
	
	public Engine(String baseDir, ClassLoader classLoader) {
		this(new File(baseDir), classLoader);
	}

	@Override
	public boolean isStarted() {
		return isStarted;
	}

	@Override
	public void startup() {
		log.info("Engine startup initiated.");
		if (isStarted)
			throw new IllegalStateException("Can't startup engine which is already started.");
		
		init();
		connectSystemHandlers();
		
		JournalStore store = journalsStorage.nextStore();
		transactionsJournaler.startWriting(journalsStorage.channel(store.getTransactionCommitsAddress(),
				config.channelOptions(), config.preallocationSize()));
		eventsJournaler.startWriting(journalsStorage.channel(store.getEventsCommitsAddress(),
				config.channelOptions(), config.preallocationSize()));
		
		eventPublisher.getPipe().start();
		workflowEngine.init();
		viewsProcessor.process(repository);
		workflowEngine.setLastTransactionId(restorer.restore(repository).getLastTransactionId());
		
		workflowEngine.getPipe().sync();
		eventPublisher.getPipe().sync();
		
		log.info("Engine is started.");
		isStarted = true;
	}

	@Override
	public void shutdown() {
		log.info("Shutting down engine.");
		isStarted = false;
		
		workflowEngine.shutdown();
		eventPublisher.getPipe().shutdown();
		
		interceptors.getInterceptors(TransactionStage.JOURNALING).forEach(TransactionInterceptor::destroy);
		interceptors.getInterceptors(TransactionStage.REPLICATION).forEach(TransactionInterceptor::destroy);
		interceptors.getInterceptors(TransactionStage.TRANSACTION).forEach(TransactionInterceptor::destroy);
		
		transactionsJournaler.destroy();
		eventsJournaler.destroy();
		
		eventsManager.close();
		
		snapshotterIntervalExecutor.shutdown();
		
		if (config.revenoSnapshotting().snapshotAtShutdown()) {
			log.info("Preforming shutdown snapshotting...");
			snapshotAll();
		}
		if (repository instanceof Destroyable) {
			((Destroyable)repository).destroy();
		}
		
		log.info("Engine was stopped.");
	}

	@Override
	public RevenoManager domain() {
		return new RevenoManager() {
			protected RepositorySnapshotter lastSnapshotter;
			
			@Override
			public DirectTransactionBuilder transaction(String name,
					BiConsumer<AbstractDynamicTransaction, TransactionContext> handler) {
				return new DirectTransactionBuilder(name, handler, serializer, transactionsManager,
						commandsManager, classLoader);
			}
			
			@Override
			public <E, V> void viewMapper(Class<E> entityType, Class<V> viewType, ViewsMapper<E, V> mapper) {
				viewsManager.register(entityType, viewType, mapper);
			}
			
			@Override
			public <T> void transactionAction(Class<T> transaction, BiConsumer<T, TransactionContext> handler) {
				serializer.registerTransactionType(transaction);
				transactionsManager.registerTransaction(transaction, handler);
			}
			
			@Override
			public <T> void transactionWithCompensatingAction(Class<T> transaction,
															  BiConsumer<T, TransactionContext> handler,
															  BiConsumer<T, TransactionContext> compensatingAction) {
				serializer.registerTransactionType(transaction);
				transactionsManager.registerTransaction(transaction, handler);
				transactionsManager.registerTransaction(transaction, compensatingAction, true);
			}
			
			@Override
			public RevenoManager snapshotWith(RepositorySnapshotter snapshotter) {
				snapshotsManager.registerSnapshotter(snapshotter);
				lastSnapshotter = snapshotter;
				return this;
			}
			
			@Override
			public void restoreWith(RepositorySnapshotter snapshotter) {
				restoreWith = snapshotter;
			}
			
			@Override
			public void andRestoreWithIt() {
				restoreWith = lastSnapshotter;
			}
			
			@Override
			public void resetSnapshotters() {
				snapshotsManager.resetSnapshotters();
			}
			
			@Override
			public <C> void command(Class<C> commandType, BiConsumer<C, CommandContext> handler) {
				serializer.registerTransactionType(commandType);
				commandsManager.register(commandType, handler);
			}
			
			@Override
			public <C, U> void command(Class<C> commandType, Class<U> resultType, BiFunction<C, CommandContext, U> handler) {
				serializer.registerTransactionType(commandType);
				commandsManager.register(commandType, resultType, handler);
			}

			@Override
			public void serializeWith(List<TransactionInfoSerializer> serializers) {
				serializer = new SerializersChain(serializers);
			}
		};
	}

	@Override
	public QueryManager query() {
		return viewsStorage;
	}

	@Override
	public EventsManager events() {
		return eventsManager;
	}

	@Override
	public ClusterManager cluster() {
		return null;
	}

	@Override
	public Configuration config() {
		return config;
	}

	@Override
	public <R> CompletableFuture<Result<? extends R>> executeCommand(Object command) {
		checkIsStarted();
		
		return workflowEngine.getPipe().execute(command);
	}

	@Override
	public CompletableFuture<EmptyResult> performCommands(List<Object> commands) {
		checkIsStarted();
		
		return workflowEngine.getPipe().process(commands);
	}
	
	@Override
	public <R> CompletableFuture<Result<? extends R>> execute(DynamicCommand command, Map<String, Object> args) {
		try {
			return executeCommand(command.newCommand(args));
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException("Failed to execute command.", e);
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <R> R executeSync(DynamicCommand command, Map<String, Object> args) {
		try {
			Result<? extends R> r = (Result<? extends R>) execute(command, args).get();
			if (!r.isSuccess()) {
				throw new RuntimeException("Failed to execute command.", r.getException());
			}

			return r.getResult();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}
	
	public InterceptorCollection interceptors() {
		return interceptors;
	}
	
	protected void checkIsStarted() {
		if (!isStarted)
			throw new IllegalStateException("The Engine must be started first.");
	}
	
	protected WriteableRepository repository() {
		return new HashMapRepository(/*1_000, 0.5f*/);
	}
	
	protected void init() {
		EngineEventsContext eventsContext = new EngineEventsContext().serializer(eventsSerializer)
				.eventsCommitBuilder(eventBuilder).eventsJournaler(eventsJournaler).manager(eventsManager);
		repository = factory.create(loadLastSnapshot());
		viewsProcessor = new ViewsProcessor(viewsManager, viewsStorage);
		processor = new DisruptorTransactionPipeProcessor(txBuilder, config.cpuConsumption(), config.revenoDisruptor().bufferSize(), executor);
		eventProcessor = new DisruptorEventPipeProcessor(CpuConsumption.NORMAL, config.revenoDisruptor().bufferSize(), eventExecutor);
		roller = new JournalsRoller(transactionsJournaler, eventsJournaler, journalsStorage);
		eventPublisher = new EventPublisher(eventProcessor, eventsContext);
		EngineWorkflowContext workflowContext = new EngineWorkflowContext().serializers(serializer).repository(repository)
				.viewsProcessor(viewsProcessor).transactionsManager(transactionsManager).commandsManager(commandsManager)
				.eventPublisher(eventPublisher).transactionCommitBuilder(txBuilder).transactionJournaler(transactionsJournaler)
				.idGenerator(idGenerator).roller(roller).snapshotsManager(snapshotsManager).interceptorCollection(interceptors)
				.configuration(config);
		workflowEngine = new WorkflowEngine(processor, workflowContext, config.modelType());
		restorer = new DefaultSystemStateRestorer(journalsStorage, workflowContext, eventsContext, workflowEngine);
	}
	
	protected Optional<RepositoryData> loadLastSnapshot() {
		if (restoreWith != null && restoreWith.hasAny()) {
			return Optional.of(restoreWith.load());
		}
		return Optional.ofNullable(snapshotsManager.getAll().stream()
						.filter(RepositorySnapshotter::hasAny).findFirst()
						.map(RepositorySnapshotter::load).orElse(null));
	}
	
	protected void connectSystemHandlers() {
		domain().transactionAction(NextIdTransaction.class, idGenerator);
		if (config.revenoSnapshotting().snapshotEvery() != -1) {
			TransactionInterceptor nTimeSnapshotter = new SnapshottingInterceptor(config, snapshotsManager, snapshotStorage,
					roller, repositorySerializer);
			interceptors.add(TransactionStage.TRANSACTION, nTimeSnapshotter);
			interceptors.add(TransactionStage.JOURNALING, nTimeSnapshotter);
		}
	}
	
	protected TxRepository createRepository() {
		switch (config.modelType()) {
		case IMMUTABLE : return new SnapshotBasedModelRepository(repository());
		case MUTABLE : return new MutableModelRepository(repository(), new SerializersChain(classLoader), classLoader);
		}
		return null;
	}
	
	protected void snapshotAll() {
		snapshotsManager.getAll().forEach(s -> s.snapshot(repository.getData(), s.prepare()));
	}

	protected volatile boolean isStarted = false;
	protected TxRepository repository;
	protected SerializersChain serializer;
	protected SystemStateRestorer restorer;
	protected ViewsProcessor viewsProcessor;
	protected WorkflowEngine workflowEngine;
	protected EventPublisher eventPublisher;
	protected TransactionPipeProcessor<ProcessorContext> processor;
	protected PipeProcessor<Event> eventProcessor;
	protected JournalsRoller roller;
	
	protected RepositorySnapshotter restoreWith;
	
	protected RepositoryDataSerializer repositorySerializer = new DefaultJavaSerializer(getClass().getClassLoader());
	protected Journaler transactionsJournaler = new DefaultJournaler();
	protected Journaler eventsJournaler = new DefaultJournaler();
	protected EventsInfoSerializer eventsSerializer = new SimpleEventsSerializer();
	protected TransactionCommitInfo.Builder txBuilder = new TransactionCommitInfoImpl.PojoBuilder();
	protected EventsCommitInfo.Builder eventBuilder = new EventsCommitInfoImpl.PojoBuilder();
	protected EventHandlersManager eventsManager = new EventHandlersManager();
	protected ViewsDefaultStorage viewsStorage = new ViewsDefaultStorage();
	protected ViewsManager viewsManager = new ViewsManager();
	protected TransactionsManager transactionsManager = new TransactionsManager();
	protected CommandsManager commandsManager = new CommandsManager();
	protected InterceptorCollection interceptors = new InterceptorCollection();
	
	protected DefaultIdGenerator idGenerator = new DefaultIdGenerator();
	
	protected RevenoConfiguration config = new RevenoConfiguration();
	protected ClassLoader classLoader;
	protected JournalsStorage journalsStorage;
	protected FoldersStorage foldersStorage;
	protected SnapshotStorage snapshotStorage;
	protected SnapshottersManager snapshotsManager;
	
	protected final ExecutorService executor = Executors.newFixedThreadPool(7);
	protected final ExecutorService eventExecutor = Executors.newFixedThreadPool(3);
	protected final ScheduledExecutorService snapshotterIntervalExecutor = Executors.newSingleThreadScheduledExecutor();
	protected static final Logger log = LoggerFactory.getLogger(Engine.class);
	
	protected final TxRepositoryFactory factory = (repositoryData) -> {
		final TxRepository repository = createRepository();
		repositoryData.ifPresent(d -> repository.load(d.data));
		return repository;
	};
	
}
