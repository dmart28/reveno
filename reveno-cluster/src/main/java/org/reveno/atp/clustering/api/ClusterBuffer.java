package org.reveno.atp.clustering.api;

import org.reveno.atp.clustering.api.ClusterConnector;
import org.reveno.atp.core.api.channel.Buffer;

/**
 * Buffer is used as replication mechanism only, so it can be anything,
 * while {@link ClusterConnector} is used only for failover inter-cluster
 * communications.
 *
 * So, absolutely different mechanisms might be employed at the same time
 * for max performance and zero-copy and latency, and at the same time not
 * loosing usability and simpleness of overall solution.
 */
public interface ClusterBuffer extends Buffer {

    void send();

}
