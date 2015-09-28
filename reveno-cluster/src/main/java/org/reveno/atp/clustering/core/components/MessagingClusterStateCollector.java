package org.reveno.atp.clustering.core.components;

import org.reveno.atp.clustering.api.Address;
import org.reveno.atp.clustering.api.Cluster;
import org.reveno.atp.clustering.api.ClusterView;
import org.reveno.atp.clustering.api.message.Message;
import org.reveno.atp.clustering.core.RevenoClusterConfiguration;
import org.reveno.atp.clustering.core.api.ClusterExecutor;
import org.reveno.atp.clustering.core.api.ClusterState;
import org.reveno.atp.clustering.core.api.MessagesReceiver;
import org.reveno.atp.clustering.core.messages.NodeStateMessage;
import org.reveno.atp.clustering.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class MessagingClusterStateCollector implements ClusterExecutor<ClusterState>, MessagesReceiver {

    @Override
    public ClusterState execute(ClusterView view) {
        LOG.info("Cluster state collection [view: {}, nodes: {}]", view.viewId(), view.members());

        if (allStatesReceived(view)) {
            Optional<NodeStateMessage> latestTransactionId = nodesStates.values().stream()
                    .filter(m -> view.members().contains(m.address()))
                    .filter(m -> m.viewId == view.viewId())
                    .max(STATE_MESSAGE_COMPARATOR);
            if (latestTransactionId.isPresent()) {
                LOG.info("Cluster state collection.");

                NodeStateMessage stateMessage = latestTransactionId.get();
                if (stateMessage.transactionId > transactionId.get()) {
                    return new ClusterState(true, stateMessage.transactionId, Optional.of(stateMessage.address()));
                } else {
                    return new ClusterState(true, transactionId.get(), Optional.empty());
                }
            } else {
                LOG.trace("Sync node not found [view: {}; txId: {}; states: {}]", view.viewId(),
                        transactionId.get(), nodesStates);
                return new ClusterState(false, 0, Optional.empty());
            }
        } else {
            LOG.trace("Not all states received [view: {}; states: {}]", view.viewId(), nodesStates);
            if (view.viewId() == cluster.view().viewId()) {
                return execute(view);
            } else {
                return new ClusterState(false, 0, Optional.empty());
            }
        }
    }

    @Override
    public <T extends Message> void onMessage(T message) {
        nodesStates.put(message.address(), (NodeStateMessage) message);
    }

    @Override
    public Set<Integer> interestedTypes() {
        return SUBSCRIPTION;
    }

    protected boolean allStatesReceived(ClusterView view) {
        NodeStateMessage message = new NodeStateMessage(view.viewId(), transactionId.get());
        cluster.gateway().send(view.members(), message, cluster.gateway().oob());
        return Utils.waitFor(() -> nodesStates.values().containsAll(view.members()) && nodesStates.entrySet()
                        .stream()
                        .filter(kv -> view.members().contains(kv.getKey()))
                        .filter(kv -> kv.getValue().viewId == view.viewId()).count() == view.members().size(),
                config.revenoTimeouts().ackTimeout());
    }

    public MessagingClusterStateCollector(Cluster cluster, Supplier<Long> transactionId, RevenoClusterConfiguration config) {
        this.cluster = cluster;
        this.transactionId = transactionId;
        this.config = config;
    }

    protected Cluster cluster;
    protected Supplier<Long> transactionId;
    protected RevenoClusterConfiguration config;

    protected Map<Address, NodeStateMessage> nodesStates = new ConcurrentHashMap<>();

    protected static final Logger LOG = LoggerFactory.getLogger(MessagingClusterStateCollector.class);
    protected static final Set<Integer> SUBSCRIPTION = new HashSet<Integer>() {{
        add(NodeStateMessage.TYPE);
    }};
    protected static final Comparator<NodeStateMessage> STATE_MESSAGE_COMPARATOR = (a, b) -> {
        if (a.transactionId > b.transactionId)
            return 1;
        else if (a.transactionId < b.transactionId)
            return -1;
        else
            return 0;
    };
}
