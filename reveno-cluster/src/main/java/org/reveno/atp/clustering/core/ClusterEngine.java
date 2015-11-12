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
import org.reveno.atp.clustering.core.providers.MulticastAllProvider;
import org.reveno.atp.clustering.core.providers.UnicastAllProvider;
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

    public ClusterEngine(File baseDir, ClassLoader classLoader, ClusterProvider clusterProvider) {
        super(baseDir, classLoader);
        this.clusterProvider = clusterProvider;
    }

    public ClusterEngine(File baseDir) {
        super(baseDir);
    }

    public ClusterEngine(String baseDir) {
        super(baseDir);
    }

    public ClusterEngine(String baseDir, ClusterProvider clusterProvider) {
        super(baseDir);
        this.clusterProvider = clusterProvider;
    }

    public ClusterEngine(FoldersStorage foldersStorage, JournalsStorage journalsStorage, SnapshotStorage snapshotStorage,
                         ClassLoader classLoader, ClusterProvider clusterProvider) {
        super(foldersStorage, journalsStorage, snapshotStorage, classLoader);
        this.clusterProvider = clusterProvider;
    }

    public ClusterEngine(FoldersStorage foldersStorage, JournalsStorage journalsStorage, SnapshotStorage snapshotStorage,
                         ClassLoader classLoader, ClusterProvider clusterProvider, StorageTransferServer server) {
        this(foldersStorage, journalsStorage, snapshotStorage, classLoader, clusterProvider);
        this.storageTransferServer = server;
    }


    public ClusterConfiguration clusterConfiguration() {
        return configuration;
    }

    @Override
    public void startup() {
        if (clusterProvider == null) {
            switch (configuration.commandsXmitTransport()) {
                case UNICAST: clusterProvider = new UnicastAllProvider(); break;
                case MULTICAST: clusterProvider = new MulticastAllProvider(); break;
            }
        }

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
        failoverExecutor.lastTransactionId(workflowEngine::getLastTransactionId);
        failoverExecutor.marshaller(marshaller());
        failoverExecutor.failoverListener(failoverWaiter::countDown);

        failoverExecutor.init();

        buffer.connect();
        cluster.connect();

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

    public boolean isElectedInCluster() {
        return cluster != null && failoverExecutor != null && failoverExecutor.lastView().viewId() > 0;
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
    protected ClusterProvider clusterProvider;

    protected StorageTransferServer storageTransferServer;
    protected ClusterBuffer buffer;
    protected Cluster cluster;
    protected ClusterFailoverManager failoverManager;
    protected FailoverExecutor failoverExecutor;
}
