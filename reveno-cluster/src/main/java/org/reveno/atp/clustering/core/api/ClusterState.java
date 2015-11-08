package org.reveno.atp.clustering.core.api;

import org.reveno.atp.clustering.core.messages.NodeState;

import java.util.Optional;

public class ClusterState {
    public final boolean failed;
    public final long currentTransactionId;
    public final Optional<NodeState> latestNode;

    public ClusterState(boolean failed, long currentTransactionId, Optional<NodeState> latestNode) {
        this.failed = failed;
        this.currentTransactionId = currentTransactionId;
        this.latestNode = latestNode;
    }
}
