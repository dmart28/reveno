package org.reveno.atp.clustering.core;

import org.reveno.atp.clustering.api.*;
import org.reveno.atp.clustering.api.message.Marshaller;
import org.reveno.atp.clustering.api.message.Message;
import org.reveno.atp.clustering.core.api.ClusterExecutor;
import org.reveno.atp.clustering.core.api.ClusterState;
import org.reveno.atp.clustering.core.api.ElectionResult;
import org.reveno.atp.clustering.core.api.MessagesReceiver;
import org.reveno.atp.clustering.core.components.GroupBarrier;
import org.reveno.atp.clustering.core.components.StorageTransferModelSync;
import org.reveno.atp.clustering.core.api.StorageTransferServer;
import org.reveno.atp.clustering.core.marshallers.JsonMarshaller;
import org.reveno.atp.clustering.exceptions.FailoverAbortedException;
import org.reveno.atp.utils.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class FailoverExecutor {

    public void init() {
        Preconditions.checkNotNull(leaderElector, "LeaderElector must be provided.");
        Preconditions.checkNotNull(clusterStateCollector, "ClusterStateCollector must be provided.");
        Preconditions.checkNotNull(modelSynchronizer, "ModelSynchronizer must be provided.");

        cluster.listenEvents(this::onClusterEvent);
        cluster.marshallWith(marshaller);
    }

    public void leaderElector(ClusterExecutor<ElectionResult, Void> leaderElector) {
        this.leaderElector = leaderElector;
        if (leaderElector instanceof MessagesReceiver) {
            subscribe((MessagesReceiver) leaderElector);
        }
    }

    public void clusterStateCollector(ClusterExecutor<ClusterState, Void> clusterStateCollector) {
        this.clusterStateCollector = clusterStateCollector;
        if (clusterStateCollector instanceof MessagesReceiver) {
            subscribe((MessagesReceiver) clusterStateCollector);
        }
    }

    public void modelSynchronizer(ClusterExecutor<Boolean, StorageTransferModelSync.TransferContext> modelSynchronizer) {
        this.modelSynchronizer = modelSynchronizer;
        if (modelSynchronizer instanceof MessagesReceiver) {
            subscribe((MessagesReceiver) modelSynchronizer);
        }
    }

    public void snapshotMaker(Runnable snapshotMaker) {
        this.snapshotMaker = snapshotMaker;
    }

    public void replayer(Supplier<Long> replayer) {
        this.replayer = replayer;
    }

    public void marshaller(Marshaller marshaller) {
        this.marshaller = marshaller;
    }

    public void failoverListener(Runnable listener) {
        this.failoverListener = listener;
    }

    public void subscribe(MessagesReceiver... receivers) {
        for (MessagesReceiver receiver : receivers) {
            for (Integer type : receiver.interestedTypes()) {
                if (receiver.filter().isPresent()) {
                    cluster.gateway().receive(type, receiver.filter().get(), receiver::onMessage);
                } else {
                    cluster.gateway().receive(type, receiver::onMessage);
                }
            }
        }
    }


    protected void onClusterEvent(ClusterEvent event) {
        if (event == ClusterEvent.MEMBERSHIP_CHANGED) {
            electorExecutor.submit(this::process);
        }
    }

    protected void process() {
        final ClusterView view = cluster.view();
        if (!isQuorum(view)) {
            LOG.info("Failover process end: not a quorum [members: {}]", view.members());
        }
        try {
            if (failoverManager.isMaster()) {
                failoverManager.block();
            }
            waitOnBarrier(view, "start");
            buffer.unlockIncoming();
            storageServer.fixJournals(view);
            if (config.revenoSync().mode() == SyncMode.SNAPSHOT) {
                snapshotMaker.run();
            }

            ElectionResult election = leaderElector.execute(view);
            if (election.failed) {
                throw new FailoverAbortedException("Unable to complete voting, restarting.");
            }
            waitOnBarrier(view, "election");

            ClusterState state = clusterStateCollector.execute(view);
            if (state.failed) {
                throw new FailoverAbortedException("Unable to gather cluster state, restarting.");
            }

            waitOnBarrier(view, "cluster_state");
            if (election.isMaster && !state.latestNode.isPresent() && !config.revenoSync().waitAllNodesSync()) {
                failoverManager.unblock();
            }
            if (state.latestNode.isPresent()) {
                modelSynchronizer.execute(view, new StorageTransferModelSync.TransferContext(state.currentTransactionId,
                        state.latestNode.get()));
            }

            waitOnBarrier(view, "sync");

            if (state.latestNode.isPresent()) {
                replayer.get();
            }

            waitOnBarrier(view, "replay");

            failoverManager.setMaster(election.isMaster);
            if (failoverManager.isBlocked()) {
                failoverManager.unblock();
            }
        } catch (Throwable t) {
            LOG.info("Leadership election is failed for view: {}", view);

            if (failoverManager.isMaster() && !failoverManager.isBlocked()) {
                failoverManager.block();
            }
            buffer.lockIncoming();
            buffer.erase();
            replayer.get();
            failoverManager.setMaster(false);

            onClusterEvent(ClusterEvent.MEMBERSHIP_CHANGED);
        }
    }

    protected void waitOnBarrier(ClusterView view, String name) {
        final GroupBarrier barrier = new GroupBarrier(cluster, view, name);
        if (!barrier.waitOn()) {
            throw new FailoverAbortedException(String.format("Timeout wait on barrier [%s] in view [%s].", name, view));
        }
    }

    protected boolean isQuorum(ClusterView view) {
        return view.members().size() >= config.clusterNodeAddresses().size() / 2;
    }

    public FailoverExecutor(Cluster cluster, ClusterFailoverManager failoverManager, StorageTransferServer storageServer,
                            RevenoClusterConfiguration config) {
        this.cluster = cluster;
        this.storageServer = storageServer;
        this.config = config;

        this.buffer = failoverManager.buffer();
        this.failoverManager = failoverManager;
    }

    protected Cluster cluster;
    protected ClusterBuffer buffer;
    protected RevenoClusterConfiguration config;
    protected ClusterFailoverManager failoverManager;
    protected StorageTransferServer storageServer;
    protected Runnable snapshotMaker = () -> {};
    protected Supplier<Long> replayer = () -> 0L;
    protected ClusterExecutor<ElectionResult, Void> leaderElector;
    protected ClusterExecutor<ClusterState, Void> clusterStateCollector;
    protected ClusterExecutor<Boolean, StorageTransferModelSync.TransferContext> modelSynchronizer;
    protected Runnable failoverListener;

    protected Marshaller marshaller = new JsonMarshaller();
    protected ExecutorService electorExecutor = Executors.newSingleThreadExecutor();

    protected static final Logger LOG = LoggerFactory.getLogger(FailoverExecutor.class);
}
