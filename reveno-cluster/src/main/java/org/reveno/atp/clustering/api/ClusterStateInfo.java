package org.reveno.atp.clustering.api;

public interface ClusterStateInfo {
    boolean isMaster();
    boolean isBlocked();
    ClusterView currentView();
}
