package org.reveno.atp.clustering.api;

import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.api.serialization.TransactionInfoSerializer;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

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

    void onView(ClusterView view);

    void messageNotifier(TransactionInfoSerializer serializer, Consumer<List<Object>> listener);

    void failoverNotifier(Consumer<ClusterEvent> listener);

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
