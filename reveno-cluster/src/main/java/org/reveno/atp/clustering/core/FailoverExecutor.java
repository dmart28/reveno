package org.reveno.atp.clustering.core;

import org.reveno.atp.clustering.api.Cluster;
import org.reveno.atp.clustering.api.ClusterEvent;
import org.reveno.atp.clustering.api.ClusterView;
import org.reveno.atp.clustering.api.message.Marshaller;
import org.reveno.atp.clustering.api.message.Message;
import org.reveno.atp.clustering.core.api.ClusterExecutor;
import org.reveno.atp.clustering.core.api.ClusterState;
import org.reveno.atp.clustering.core.api.ElectionResult;
import org.reveno.atp.clustering.core.api.MessagesReceiver;
import org.reveno.atp.clustering.core.buffer.ClusterBufferFactory;
import org.reveno.atp.clustering.core.components.StorageTransferModelSync;
import org.reveno.atp.clustering.core.marshallers.JsonMarshaller;
import org.reveno.atp.core.engine.WorkflowEngine;
import org.reveno.atp.utils.Preconditions;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FailoverExecutor {

    public void init() {
        Preconditions.checkNotNull(leaderElector, "LeaderElector must be provided.");
        Preconditions.checkNotNull(clusterStateCollector, "ClusterStateCollector must be provided.");
        Preconditions.checkNotNull(modelSynchronizer, "ModelSynchronizer must be provided.");

        if (!cluster.isConnected()) {
            cluster.connect();
        }
        cluster.listenEvents(this::onClusterEvent);
        cluster.marshallWith(Message.class, marshaller);
    }

    public void shutdown() {
        cluster.disconnect();
    }

    public void leaderElector(ClusterExecutor<ElectionResult, Void> leaderElector) {
        this.leaderElector = leaderElector;
    }

    public void clusterStateCollector(ClusterExecutor<ClusterState, Void> clusterStateCollector) {
        this.clusterStateCollector = clusterStateCollector;
    }

    public void modelSynchronizer(ClusterExecutor<Boolean, StorageTransferModelSync.TransferContext> modelSynchronizer) {
        this.modelSynchronizer = modelSynchronizer;
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

    public void marshaller(Marshaller marshaller) {
        this.marshaller = marshaller;
    }


    protected void onClusterEvent(ClusterEvent event) {
        if (event == ClusterEvent.MEMBERSHIP_CHANGED) {
            final ClusterView currentView = cluster.view();
            electorExecutor.submit(() -> {

            });
        }
    }

    public FailoverExecutor(WorkflowEngine engine, Cluster cluster, ClusterBufferFactory bufferFactory) {
        this.engine = engine;
        this.cluster = cluster;

        this.failoverManager = new ClusterFailoverManager(bufferFactory.createBuffer());
    }

    protected WorkflowEngine engine;
    protected Cluster cluster;
    protected ClusterFailoverManager failoverManager;

    protected ClusterExecutor<ElectionResult, Void> leaderElector;
    protected ClusterExecutor<ClusterState, Void> clusterStateCollector;
    protected ClusterExecutor<Boolean, StorageTransferModelSync.TransferContext> modelSynchronizer;
    protected Marshaller marshaller = new JsonMarshaller();
    protected ExecutorService electorExecutor = Executors.newSingleThreadExecutor();
}
