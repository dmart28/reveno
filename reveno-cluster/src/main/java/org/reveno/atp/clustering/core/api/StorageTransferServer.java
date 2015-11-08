package org.reveno.atp.clustering.core.api;

import org.reveno.atp.clustering.api.ClusterView;

public interface StorageTransferServer {
    void startup();

    void shutdown();

    void fixJournals(ClusterView view);
}
