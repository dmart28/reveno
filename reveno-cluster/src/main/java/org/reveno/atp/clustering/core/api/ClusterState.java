package org.reveno.atp.clustering.core.api;

import org.reveno.atp.clustering.api.Address;

import java.util.Optional;

public class ClusterState {
    public final boolean isComplete;
    public final long lastTransactionId;
    public final Optional<Address> node;

    public ClusterState(boolean isComplete, long lastTransactionId, Optional<Address> node) {
        this.isComplete = isComplete;
        this.lastTransactionId = lastTransactionId;
        this.node = node;
    }
}
