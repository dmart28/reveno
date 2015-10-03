package org.reveno.atp.clustering.core.api;

import org.reveno.atp.clustering.core.messages.NodeState;

import java.util.Optional;

public class ClusterState {
    public final boolean isComplete;
    public final Optional<NodeState> latestNode;

    public ClusterState(boolean isComplete, Optional<NodeState> latestNode) {
        this.isComplete = isComplete;
        this.latestNode = latestNode;
    }
}
