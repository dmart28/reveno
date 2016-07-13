package org.reveno.atp.clustering.api;

public interface ClusterStateInfo {
    boolean isMaster();
    boolean isBlocked();
    long electionId();
    ClusterView currentView();
}
