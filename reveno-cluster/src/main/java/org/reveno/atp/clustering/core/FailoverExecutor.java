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

import org.reveno.atp.clustering.api.*;
import org.reveno.atp.clustering.api.message.Marshaller;
import org.reveno.atp.clustering.core.api.ClusterExecutor;
import org.reveno.atp.clustering.core.api.ClusterState;
import org.reveno.atp.clustering.core.api.ElectionResult;
import org.reveno.atp.clustering.core.api.MessagesReceiver;
import org.reveno.atp.clustering.core.components.GroupBarrier;
import org.reveno.atp.clustering.core.components.StorageTransferModelSync;
import org.reveno.atp.clustering.core.api.StorageTransferServer;
import org.reveno.atp.clustering.core.messages.ForceElectionProcess;
import org.reveno.atp.clustering.exceptions.FailoverAbortedException;
import org.reveno.atp.commons.NamedThreadFactory;
import org.reveno.atp.core.JournalsManager;
import org.reveno.atp.utils.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.function.Supplier;

public class FailoverExecutor {

    /**
     * Used by internal queue of ThreadPoolExecutor, to make sure we won't loose
     * any new Leadership Election process requests.
     */
    private static final int NUM_JOBS_TO_QUEUE = 500;

    public void init() {
        Preconditions.checkNotNull(leaderElector, "LeaderElector must be provided.");
        Preconditions.checkNotNull(clusterStateCollector, "ClusterStateCollector must be provided.");
        Preconditions.checkNotNull(modelSynchronizer, "ModelSynchronizer must be provided.");

        cluster.listenEvents(this::onClusterEvent);
        cluster.marshallWith(marshaller);
        cluster.gateway().receive(ForceElectionProcess.TYPE, f -> electorExecutor.submit(() -> process(true)));
        buffer.failoverNotifier(e -> {
            cluster.gateway().send(lastView.members(), new ForceElectionProcess(), cluster.gateway().oob());
            electorExecutor.submit(() -> process(true));
        });
    }

    public void stop() {
        isStopped = true;
        electorExecutor.shutdown();
        try {
            electorExecutor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public void startElectionProcess() {
        onClusterEvent(ClusterEvent.MEMBERSHIP_CHANGED);
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

    public void lastTransactionId(Supplier<Long> lastTransactionId) {
        this.lastTransactionId = lastTransactionId;
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

    public ClusterView lastView() {
        return lastView;
    }


    protected void onClusterEvent(ClusterEvent event) {
        if (event == ClusterEvent.MEMBERSHIP_CHANGED) {
            buffer.onView(cluster.view());
            electorExecutor.submit(() -> process(false));
        }
    }

    protected void process(boolean force) {
        LOG.info("Cluster merge process started (forced: {})", force);
        ClusterView view;
        try {
            view = cluster.view();
            if (!force && lastView.equals(view)) {
                LOG.info("Cluster is already up-to-date.");
                notifyListener();
                return;
            }
            if (!isQuorum(view)) {
                LOG.info("Failover process end: not a quorum [members: {}]", view.members());
                notifyListener();
                return;
            }
        } catch (Throwable t) {
            LOG.error("Unexpected process error.", t);
            return;
        }
        try {
            long t1 = System.currentTimeMillis();
            blockIfMaster();
            waitOnBarrier(view, "start");

            buffer.lockIncoming();
            waitOnBarrier(view, "lock-buffer");

            rollAndFixJournals(view);
            ElectionResult election = leadershipElection(view);
            waitOnBarrier(view, "election");

            ClusterState state = clusterStateCollection(view);
            waitOnBarrier(view, "cluster-state");

            buffer.unlockIncoming();
            waitOnBarrier(view, "unlock-buffer");

            unblockMasterOrSynchronizeSlave(view, election, state);
            waitOnBarrier(view, "sync", config.revenoElectionTimeouts().syncBarrierTimeoutNanos());

            // block master only if it was unblocked because of
            // option waitAllNodesSync switched off
            blockIfMaster(() -> !config.revenoDataSync().waitAllNodesSync());
            waitOnBarrier(view, "block-if-master");

            // TODO probably better to replay only if anything was received during sync stage
            replay(state);
            waitOnBarrier(view, "replay");

            if (!election.isMaster) {
                unblock();
            }
            waitOnBarrier(view, "unblock-slaves");

            if (election.isMaster) {
                unblock();
            }
            waitOnBarrier(view, "unblock-master");
            lastView = view;
            makeMasterIfElected(election);

            LOG.info("Election Process Time: {} ms", System.currentTimeMillis() - t1);
            notifyListener();
        } catch (Throwable t) {
            LOG.error("Leadership election is failed for view: {}, {}", view, t.getMessage());

            blockAndLock();
            buffer.erase();
            if (!isStopped) {
                replayer.get();
            }
            failoverManager.setMaster(false);

            await();
            if (!isStopped) {
                onClusterEvent(ClusterEvent.MEMBERSHIP_CHANGED);
            }
        }
    }

    protected void makeMasterIfElected(ElectionResult election) {
        failoverManager.setMaster(election.isMaster);
    }

    protected void blockAndLock() {
        if (failoverManager.isMaster() && !failoverManager.isBlocked()) {
            failoverManager.block();
        }
        buffer.lockIncoming();
    }

    protected void unblock() {
        if (failoverManager.isBlocked()) {
            failoverManager.unblock();
        }
    }

    protected void replay(ClusterState state) {
        if (state.latestNode.isPresent()) {
            replayer.get();
        }
    }

    protected void unblockMasterOrSynchronizeSlave(ClusterView view, ElectionResult election, ClusterState state) {
        if (election.isMaster && !state.latestNode.isPresent() && !config.revenoDataSync().waitAllNodesSync()) {
            failoverManager.setMaster(true);
            failoverManager.unblock();
        }
        if (state.latestNode.isPresent()) {
            modelSynchronizer.execute(view, new StorageTransferModelSync.TransferContext(state.currentTransactionId,
                    state.latestNode.get()));
        }
    }

    protected ClusterState clusterStateCollection(ClusterView view) {
        ClusterState state = clusterStateCollector.execute(view);
        if (state.failed) {
            throw new FailoverAbortedException("Unable to gather cluster state, restarting.");
        }
        return state;
    }

    protected ElectionResult leadershipElection(ClusterView view) {
        ElectionResult election = leaderElector.execute(view);
        if (election.failed) {
            throw new FailoverAbortedException("Unable to complete voting, restarting.");
        }
        return election;
    }

    protected void rollAndFixJournals(ClusterView view) {
        buffer.erase();
        if (config.revenoDataSync().mode() == SyncMode.SNAPSHOT) {
            snapshotMaker.run();
        }
        storageServer.fixJournals(view);
        journalsManager.roll(lastTransactionId.get());
    }

    protected void blockIfMaster() {
        blockIfMaster(() -> true);
    }

    protected void blockIfMaster(Supplier<Boolean> condition) {
        if (failoverManager.isMaster() && condition.get()) {
            failoverManager.block();
        }
    }

    private void notifyListener() {
        if (failoverListener != null) {
            failoverListener.run();
        }
    }

    protected void waitOnBarrier(ClusterView view, String name) {
        waitOnBarrier(view, name, config.revenoElectionTimeouts().barrierTimeoutNanos());
    }

    protected void waitOnBarrier(ClusterView view, String name, long timeout) {
        LOG.debug("Wait on barrier [{}]", name);
        final GroupBarrier barrier = new GroupBarrier(cluster, view, name, timeout);
        if (!barrier.waitOn()) {
            throw new FailoverAbortedException(String.format("Timeout wait on barrier [%s] in view [%s].", name, view));
        } else {
            LOG.debug("Reached barrier [{}]", name);
        }
    }

    protected boolean isQuorum(ClusterView view) {
        return view.members().size() != 0 && view.members().size() >= config.nodesAddresses().size() / 2;
    }

    protected void await() {
        try {
            Thread.sleep(config.revenoElectionTimeouts().ackTimeoutNanos() / 1_000_000);
        } catch (InterruptedException ignored) {
        }
    }

    public FailoverExecutor(Cluster cluster, JournalsManager journalsManager, ClusterFailoverManager failoverManager,
                            StorageTransferServer storageServer, RevenoClusterConfiguration config) {
        this.cluster = cluster;
        this.storageServer = storageServer;
        this.config = config;
        this.journalsManager = journalsManager;

        this.buffer = failoverManager.buffer();
        this.failoverManager = failoverManager;

        final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(NUM_JOBS_TO_QUEUE);
        this.electorExecutor = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS, queue, new NamedThreadFactory("fe-" + config.currentNodeAddress()));
        this.electorExecutor.setRejectedExecutionHandler((r, executor) -> {
            // this will block if the queue is full as opposed to throwing
            try {
                electorExecutor.getQueue().put(r);
            } catch (InterruptedException ignored) {
            }
        });
    }

    protected volatile boolean isStopped = false;
    protected volatile ClusterView lastView = ClusterView.EMPTY_VIEW;

    protected Cluster cluster;
    protected ClusterBuffer buffer;
    protected RevenoClusterConfiguration config;
    protected JournalsManager journalsManager;
    protected ClusterFailoverManager failoverManager;
    protected StorageTransferServer storageServer;
    protected Runnable snapshotMaker = () -> {};
    protected Supplier<Long> replayer = () -> 0L;
    protected Supplier<Long> lastTransactionId = () -> 0L;
    protected ClusterExecutor<ElectionResult, Void> leaderElector;
    protected ClusterExecutor<ClusterState, Void> clusterStateCollector;
    protected ClusterExecutor<Boolean, StorageTransferModelSync.TransferContext> modelSynchronizer;
    protected Runnable failoverListener;

    protected Marshaller marshaller;
    protected final ThreadPoolExecutor electorExecutor;

    protected static final Logger LOG = LoggerFactory.getLogger(FailoverExecutor.class);
}
