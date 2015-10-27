package org.reveno.atp.clustering.api;

import org.reveno.atp.core.api.channel.Buffer;

import java.util.function.Function;

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

    void connect();

    void disconnect();

    void messageNotifier(Function<ClusterBuffer, Boolean> listener);

    /**
     * Mode to reject all incoming data.
     */
    void lockIncoming();

    void unlockIncoming();

    /**
     * Clear all unprocessed data.
     */
    void erase();

    void prepare();

    boolean replicate();

}
