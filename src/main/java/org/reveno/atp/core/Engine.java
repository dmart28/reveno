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
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.reveno.atp.api.ClusterManager;
import org.reveno.atp.api.Configuration;
import org.reveno.atp.api.EventsManager;
import org.reveno.atp.api.RepositorySnapshooter;
import org.reveno.atp.api.Reveno;
import org.reveno.atp.api.RevenoManager;
import org.reveno.atp.api.commands.CommandContext;
import org.reveno.atp.api.commands.EmptyResult;
import org.reveno.atp.api.commands.Result;
import org.reveno.atp.api.domain.RepositoryData;
import org.reveno.atp.api.domain.WriteableRepository;
import org.reveno.atp.api.query.QueryManager;
import org.reveno.atp.api.query.ViewsMapper;
import org.reveno.atp.api.transaction.TransactionContext;
import org.reveno.atp.api.transaction.TransactionInterceptor;
import org.reveno.atp.api.transaction.TransactionStage;
import org.reveno.atp.core.api.EventPublisher;
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
import org.reveno.atp.core.disruptor.DisruptorPipeProcessor;
import org.reveno.atp.core.engine.WorkflowEngine;
import org.reveno.atp.core.engine.components.CommandsManager;
import org.reveno.atp.core.engine.components.DefaultIdGenerator;
import org.reveno.atp.core.engine.components.SnapshootingInterceptor;
import org.reveno.atp.core.engine.components.DefaultIdGenerator.NextIdTransaction;
import org.reveno.atp.core.engine.components.SerializersChain;
import org.reveno.atp.core.engine.components.TransactionsManager;
import org.reveno.atp.core.engine.processor.PipeProcessor;
import org.reveno.atp.core.events.DisruptorEventPublisher;
import org.reveno.atp.core.events.EventHandlersManager;
import org.reveno.atp.core.impl.EventsCommitInfoImpl;
import org.reveno.atp.core.impl.TransactionCommitInfoImpl;
import org.reveno.atp.core.repository.HashMapRepository;
import org.reveno.atp.core.repository.ImmutableModelRepository;
import org.reveno.atp.core.repository.MutableModelRepository;
import org.reveno.atp.core.restore.DefaultSystemStateRestorer;
import org.reveno.atp.core.serialization.DefaultJavaSerializer;
import org.reveno.atp.core.serialization.SimpleEventsSerializer;
import org.reveno.atp.core.snapshots.SnapshotsManager;
import org.reveno.atp.core.storage.FileSystemStorage;
import org.reveno.atp.core.views.ViewsDefaultStorage;
import org.reveno.atp.core.views.ViewsManager;
import org.reveno.atp.core.views.ViewsProcessor;
import org.reveno.atp.utils.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Engine implements Reveno {
	
	public Engine(FoldersStorage foldersStorage, JournalsStorage journalsStorage,
			SnapshotStorage snapshotStorage) {
		this.foldersStorage = foldersStorage;
		this.journalsStorage = journalsStorage;
		this.snapshotStorage = snapshotStorage;
	}
	
	public Engine(File baseDir) {
		FileSystemStorage storage = new FileSystemStorage(baseDir);
		this.foldersStorage = storage;
		this.journalsStorage = storage;
		this.snapshotStorage = storage;
	}
	
	public Engine(String baseDir) {
		this(new File(baseDir));
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
		transactionsJournaler.startWriting(journalsStorage.channel(store.getTransactionCommitsAddress()));
		eventsJournaler.startWriting(journalsStorage.channel(store.getEventsCommitsAddress()));
		
		eventPublisher.start();
		workflowEngine.init();
		viewsProcessor.process(mapAsMarked(repository));
		workflowEngine.setLastTransactionId(restorer.restore(repository).getLastTransactionId());
		
		log.info("Engine is started.");
		isStarted = true;
	}

	@Override
	public void shutdown() {
		log.info("Shutting down engine.");
		isStarted = false;
		
		workflowEngine.shutdown();
		eventPublisher.stop();
		
		transactionsJournaler.destroy();
		eventsJournaler.destroy();
		
		eventsManager.close();
		executor.shutdown();
		eventPublisher.shutdown();
		
		snapshooterIntervalExecutor.shutdown();
		
		if (configuration.revenoSnapshooting().snapshootAtShutdown()) {
			log.info("Preforming shutdown snapshooting...");
			snapshootAll();
		}
		
		log.info("Engine was stopped.");
	}

	@Override
	public RevenoManager domain() {
		return new RevenoManager() {
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
			public RevenoManager snapshootWith(RepositorySnapshooter snapshooter) {
				snapshotsManager.registerSnapshooter(snapshooter);
				return this;
			}
			
			@Override
			public void restoreWith(RepositorySnapshooter snapshooter) {
				restoreWith = snapshooter;
			}
			
			@Override
			public void resetSnapshooters() {
				snapshotsManager.resetSnapshooters();
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
		return configuration;
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
	
	public InterceptorCollection interceptors() {
		return interceptors;
	}
	
	protected void checkIsStarted() {
		if (!isStarted)
			throw new IllegalStateException("The Engine must be started first.");
	}
	
	protected WriteableRepository repository() {
		return new HashMapRepository(1_000, 0.5f);
	}
	
	protected void init() {
		EngineEventsContext eventsContext = new EngineEventsContext().serializer(eventsSerializer)
				.eventsCommitBuilder(eventBuilder).eventsJournaler(eventsJournaler).manager(eventsManager);
		eventPublisher = new DisruptorEventPublisher(configuration.cpuConsumption(), eventsContext);
		snapshotsManager = new SnapshotsManager(snapshotStorage, repositorySerializer);
		repository = factory.create(snapshotStorage.getLastSnapshotStore() != null ? snapshooter().load() : null);
		viewsProcessor = new ViewsProcessor(viewsManager, viewsStorage, repository);
		processor = new DisruptorPipeProcessor(configuration.cpuConsumption(), false, executor);
		roller = new JournalsRoller(transactionsJournaler, eventsJournaler, journalsStorage);
		EngineWorkflowContext workflowContext = new EngineWorkflowContext().serializers(serializer).repository(repository)
				.viewsProcessor(viewsProcessor).transactionsManager(transactionsManager).commandsManager(commandsManager)
				.eventPublisher(eventPublisher).transactionCommitBuilder(txBuilder).transactionJournaler(transactionsJournaler)
				.idGenerator(idGenerator).roller(roller).snapshotsManager(snapshotsManager).interceptorCollection(interceptors);
		workflowEngine = new WorkflowEngine(processor, workflowContext);
		restorer = new DefaultSystemStateRestorer(journalsStorage, workflowContext, eventsContext, workflowEngine);
	}
	
	protected Map<Class<?>, Set<Long>> mapAsMarked(TxRepository repository) {
		Map<Class<?>, Set<Long>> marked = MapUtils.repositorySet();
		repository.getData().data.forEach((k, v) -> marked.put(k, v.keySet()));
		return marked;
	}
	
	protected void connectSystemHandlers() {
		domain().transactionAction(NextIdTransaction.class, idGenerator);
		if (configuration.revenoSnapshooting().snapshootEvery() != -1) {
			TransactionInterceptor nTimeSnapshooter = new SnapshootingInterceptor(configuration, snapshotsManager,
					roller, repositorySerializer);
			interceptors.add(TransactionStage.TRANSACTION, nTimeSnapshooter);
			interceptors.add(TransactionStage.JOURNALING, nTimeSnapshooter);
		}
	}
	
	protected RepositorySnapshooter snapshooter() {
		if (restoreWith != null) 
			return restoreWith;
		else 
			return snapshotsManager.defaultSnapshooter();
	}
	
	protected void snapshootAll() {
		snapshotsManager.getAll().forEach(s -> s.snapshoot(repository.getData()));
	}
	
	protected final TxRepositoryFactory factory = new TxRepositoryFactory() {
		@Override
		public TxRepository create(RepositoryData data) {
			TxRepository repository = null;
			switch (configuration.modelType()) {
			case IMMUTABLE : repository = new ImmutableModelRepository(repository()); break;
			case MUTABLE : repository = new MutableModelRepository(repository()); break;
			}
			if (data != null)
				repository.load(data.data);
			return repository;
		}
		
		@Override
		public TxRepository create() {
			return create(null);
		}
	};

	protected volatile boolean isStarted = false;
	protected TxRepository repository;
	protected SystemStateRestorer restorer;
	protected SnapshotsManager snapshotsManager;
	protected ViewsProcessor viewsProcessor;
	protected WorkflowEngine workflowEngine;
	protected EventPublisher eventPublisher;
	protected PipeProcessor processor;
	protected JournalsRoller roller;
	
	protected RepositorySnapshooter restoreWith;
	
	protected RepositoryDataSerializer repositorySerializer = new DefaultJavaSerializer(getClass().getClassLoader());
	protected SerializersChain serializer = new SerializersChain();
	protected Journaler transactionsJournaler = new DefaultJournaler();
	protected Journaler eventsJournaler = new DefaultJournaler();
	protected EventsInfoSerializer eventsSerializer = new SimpleEventsSerializer();
	protected TransactionCommitInfo.Builder txBuilder = new TransactionCommitInfoImpl.PojoBuilder();
	protected EventsCommitInfo.Builder eventBuilder = new EventsCommitInfoImpl.PojoBuilder();
	protected EventHandlersManager eventsManager = new EventHandlersManager();
	protected ViewsDefaultStorage viewsStorage = new ViewsDefaultStorage(repository());
	protected ViewsManager viewsManager = new ViewsManager();
	protected TransactionsManager transactionsManager = new TransactionsManager();
	protected CommandsManager commandsManager = new CommandsManager();
	protected InterceptorCollection interceptors = new InterceptorCollection();
	
	protected DefaultIdGenerator idGenerator = new DefaultIdGenerator();
	
	protected final RevenoConfiguration configuration = new RevenoConfiguration();
	protected final JournalsStorage journalsStorage;
	protected final FoldersStorage foldersStorage;
	protected final SnapshotStorage snapshotStorage;
	
	protected final ExecutorService executor = Executors.newFixedThreadPool(8);
	protected final ScheduledExecutorService snapshooterIntervalExecutor = Executors.newSingleThreadScheduledExecutor();
	protected static final Logger log = LoggerFactory.getLogger(Engine.class);
}
