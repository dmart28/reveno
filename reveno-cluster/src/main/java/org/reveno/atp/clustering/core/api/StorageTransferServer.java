package org.reveno.atp.clustering.core.api;

import org.reveno.atp.clustering.api.ClusterView;

/**
 * Created by admitriev on 06/10/15.
 */
public interface StorageTransferServer {
    void startup();

    void shutdown();

    void fixJournals(ClusterView view);
}
