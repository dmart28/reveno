/**
 *  Copyright (c) 2015 The original author or authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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
     * Reject all incoming data.
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
