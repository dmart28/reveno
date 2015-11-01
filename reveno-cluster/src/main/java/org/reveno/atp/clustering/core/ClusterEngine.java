package org.reveno.atp.clustering.core;

import org.reveno.atp.clustering.api.Cluster;
import org.reveno.atp.clustering.api.ClusterBuffer;
import org.reveno.atp.clustering.api.ClusterConfiguration;
import org.reveno.atp.clustering.api.message.Marshaller;
import org.reveno.atp.clustering.core.api.ClusterExecutor;
import org.reveno.atp.clustering.core.api.ClusterState;
import org.reveno.atp.clustering.core.api.ElectionResult;
import org.reveno.atp.clustering.core.api.StorageTransferServer;
import org.reveno.atp.clustering.core.buffer.ClusterProvider;
import org.reveno.atp.clustering.core.components.FileStorageTransferServer;
import org.reveno.atp.clustering.core.components.MessagingClusterStateCollector;
import org.reveno.atp.clustering.core.components.MessagingMasterSlaveElector;
import org.reveno.atp.clustering.core.components.StorageTransferModelSync;
import org.reveno.atp.clustering.core.jgroups.JGroupsProvider;
import org.reveno.atp.clustering.core.marshallers.NativeMarshaller;
import org.reveno.atp.core.Engine;
import org.reveno.atp.core.api.FailoverManager;
import org.reveno.atp.core.api.storage.FoldersStorage;
import org.reveno.atp.core.api.storage.JournalsStorage;
import org.reveno.atp.core.api.storage.SnapshotStorage;
import org.reveno.atp.core.storage.FileSystemStorage;
import org.reveno.atp.utils.Exceptions;

import java.io.File;
import java.util.concurrent.CountDownLatch;

public class ClusterEngine extends Engine {
    public ClusterEngine(FoldersStorage foldersStorage, JournalsStorage journalsStorage, SnapshotStorage snapshotStorage,
                         ClassLoader classLoader) {
        super(foldersStorage, journalsStorage, snapshotStorage, classLoader);
    }

    public ClusterEngine(String baseDir, ClassLoader classLoader) {
        super(baseDir, classLoader);
    }

    public ClusterEngine(File baseDir, ClassLoader classLoader) {
        super(baseDir, classLoader);
    }

    public ClusterEngine(File baseDir, ClassLoader classLoader, ClusterProvider factory) {
        super(baseDir, classLoader);
        this.clusterProvider = factory;
    }

    public ClusterEngine(File baseDir) {
        super(baseDir);
    }

    public ClusterEngine(String baseDir) {
        super(baseDir);
    }

    public ClusterEngine(String baseDir, ClusterProvider factory) {
        super(baseDir);
        this.clusterProvider = factory;
    }

    public ClusterEngine(FoldersStorage foldersStorage, JournalsStorage journalsStorage, SnapshotStorage snapshotStorage,
                         ClassLoader classLoader, ClusterProvider factory) {
        super(foldersStorage, journalsStorage, snapshotStorage, classLoader);
        this.clusterProvider = factory;
    }

    public ClusterEngine(FoldersStorage foldersStorage, JournalsStorage journalsStorage, SnapshotStorage snapshotStorage,
                         ClassLoader classLoader, ClusterProvider factory, StorageTransferServer server) {
        this(foldersStorage, journalsStorage, snapshotStorage, classLoader, factory);
        this.storageTransferServer = server;
    }


    public ClusterConfiguration clusterConfiguration() {
        return configuration;
    }

    @Override
    public void startup() {
        final CountDownLatch failoverWaiter = new CountDownLatch(1);
        clusterProvider.initialize(configuration);
        this.buffer = clusterProvider.retrieveBuffer();
        this.cluster = clusterProvider.retrieveCluster();
        this.failoverManager = new ClusterFailoverManager(this.serializer, buffer);

        if (storageTransferServer == null) {
            this.storageTransferServer = new FileStorageTransferServer(configuration, (FileSystemStorage) journalsStorage);
        }

        super.startup();

        storageTransferServer.startup();

        failoverExecutor = new FailoverExecutor(cluster, journalsManager, failoverManager, storageTransferServer, configuration);
        failoverExecutor.snapshotMaker(this::snapshotAll);
        failoverExecutor.replayer(this::replay);
        failoverExecutor.leaderElector(leadershipExecutor());
        failoverExecutor.clusterStateCollector(clusterStateCollector());
        failoverExecutor.modelSynchronizer(transferModelSync());
        failoverExecutor.marshaller(marshaller());
        failoverExecutor.failoverListener(failoverWaiter::countDown);

        failoverExecutor.init();

        cluster.connect();
        buffer.connect();

        failoverExecutor.startElectionProcess();

        try {
            failoverWaiter.await();
        } catch (InterruptedException e) {
            throw Exceptions.runtime(e);
        }
    }

    @Override
    public void shutdown() {
        isStarted = false;
        failoverExecutor.stop();
        storageTransferServer.shutdown();
        buffer.disconnect();
        cluster.disconnect();

        super.shutdown();
    }

    @Override
    public FailoverManager failoverManager() {
        return failoverManager;
    }

    protected ClusterExecutor<ElectionResult, Void> leadershipExecutor() {
        return new MessagingMasterSlaveElector(cluster, configuration);
    }

    protected ClusterExecutor<ClusterState, Void> clusterStateCollector() {
        return new MessagingClusterStateCollector(cluster, workflowEngine::getLastTransactionId, configuration);
    }

    protected ClusterExecutor<Boolean, StorageTransferModelSync.TransferContext> transferModelSync() {
        return new StorageTransferModelSync(configuration, journalsStorage, snapshotStorage);
    }

    protected Marshaller marshaller() {
        return new NativeMarshaller();
    }

    protected long replay() {
        repository = factory.create(loadLastSnapshot());
        viewsProcessor.erase();
        viewsProcessor.process(repository);
        workflowEngine.setLastTransactionId(restorer.restore(repository).getLastTransactionId());
        workflowContext.repository(repository);
        return workflowEngine.getLastTransactionId();
    }

    protected RevenoClusterConfiguration configuration = new RevenoClusterConfiguration();
    protected ClusterProvider clusterProvider = new JGroupsProvider("classpath:/tcp.xml");

    protected StorageTransferServer storageTransferServer;
    protected ClusterBuffer buffer;
    protected Cluster cluster;
    protected ClusterFailoverManager failoverManager;
    protected FailoverExecutor failoverExecutor;
}
