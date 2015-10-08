package org.reveno.atp.clustering.core;

import org.reveno.atp.clustering.api.Cluster;
import org.reveno.atp.clustering.api.ClusterBuffer;
import org.reveno.atp.clustering.api.ClusterConfiguration;
import org.reveno.atp.clustering.api.message.Marshaller;
import org.reveno.atp.clustering.core.api.ClusterExecutor;
import org.reveno.atp.clustering.core.api.ClusterState;
import org.reveno.atp.clustering.core.api.ElectionResult;
import org.reveno.atp.clustering.core.api.StorageTransferServer;
import org.reveno.atp.clustering.core.buffer.ClusterFactory;
import org.reveno.atp.clustering.core.components.FileStorageTransferServer;
import org.reveno.atp.clustering.core.components.MessagingClusterStateCollector;
import org.reveno.atp.clustering.core.components.MessagingMasterSlaveElector;
import org.reveno.atp.clustering.core.components.StorageTransferModelSync;
import org.reveno.atp.clustering.core.jgroups.JGroupsFactory;
import org.reveno.atp.clustering.core.marshallers.JsonMarshaller;
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

    public ClusterEngine(File baseDir, ClassLoader classLoader, ClusterFactory factory) {
        super(baseDir, classLoader);
        this.clusterFactory = factory;
    }

    public ClusterEngine(File baseDir) {
        super(baseDir);
    }

    public ClusterEngine(String baseDir) {
        super(baseDir);
    }

    public ClusterEngine(String baseDir, ClusterFactory factory) {
        super(baseDir);
        this.clusterFactory = factory;
    }

    public ClusterEngine(FoldersStorage foldersStorage, JournalsStorage journalsStorage, SnapshotStorage snapshotStorage,
                         ClassLoader classLoader, ClusterFactory factory) {
        super(foldersStorage, journalsStorage, snapshotStorage, classLoader);
        this.clusterFactory = factory;
    }

    public ClusterEngine(FoldersStorage foldersStorage, JournalsStorage journalsStorage, SnapshotStorage snapshotStorage,
                         ClassLoader classLoader, ClusterFactory factory, StorageTransferServer server) {
        this(foldersStorage, journalsStorage, snapshotStorage, classLoader, factory);
        this.storageTransferServer = server;
    }


    public ClusterConfiguration clusterConfiguration() {
        return configuration;
    }

    @Override
    public void startup() {
        final CountDownLatch failoverWaiter = new CountDownLatch(1);
        this.buffer = clusterFactory.createBuffer(configuration);
        this.cluster = clusterFactory.createCluster(configuration);
        this.failoverManager = new ClusterFailoverManager(buffer);

        if (storageTransferServer == null) {
            this.storageTransferServer = new FileStorageTransferServer(configuration, (FileSystemStorage) journalsStorage);
        }

        super.startup();

        storageTransferServer.startup();

        failoverExecutor = new FailoverExecutor(cluster, failoverManager, storageTransferServer, configuration);
        failoverExecutor.init();
        failoverExecutor.snapshotMaker(this::snapshotAll);
        failoverExecutor.replayer(this::replay);
        failoverExecutor.leaderElector(leadershipExecutor());
        failoverExecutor.clusterStateCollector(clusterStateCollector());
        failoverExecutor.modelSynchronizer(transferModelSync());
        failoverExecutor.marshaller(marshaller());
        failoverExecutor.failoverListener(failoverWaiter::countDown);

        cluster.connect();
        buffer.connect();

        try {
            failoverWaiter.await();
        } catch (InterruptedException e) {
            throw Exceptions.runtime(e);
        }
    }

    @Override
    public void shutdown() {
        storageTransferServer.shutdown();
        buffer.disconnect();
        cluster.disconnect();

        super.shutdown();
    }

    @Override
    protected FailoverManager failoverManager() {
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
        return new JsonMarshaller();
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
    protected ClusterFactory clusterFactory = new JGroupsFactory();

    protected StorageTransferServer storageTransferServer;
    protected ClusterBuffer buffer;
    protected Cluster cluster;
    protected ClusterFailoverManager failoverManager;
    protected FailoverExecutor failoverExecutor;
}
