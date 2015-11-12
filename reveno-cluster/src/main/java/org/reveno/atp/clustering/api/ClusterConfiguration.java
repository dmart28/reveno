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

import java.util.List;

public interface ClusterConfiguration {

    void currentNodeAddress(Address nodeAddress);

    void clusterNodeAddresses(List<Address> nodeAddresses);

    void commandsXmitTransport(CommandsXmitTransport transport);

    MulticastConfiguration multicast();

    UnicastConfiguration unicast();

    TimeoutsConfiguration electionTimeouts();

    SyncConfiguration dataSync();

    void authToken(String authToken);

    void priorityInCluster(int priority);


    interface TimeoutsConfiguration {
        void voteTimeoutNanos(long timeout);

        void syncTimeoutNanos(long timeout);

        void ackTimeoutNanos(long timeout);

        void barrierTimeoutNanos(long timeout);

        void syncBarrierTimeoutNanos(long timeout);
    }

    interface SyncConfiguration {
        void mode(SyncMode mode);

        void threadPoolSize(int threads);

        void retries(int count);

        void port(int port);

        void waitAllNodesSync(boolean wait);
    }

    interface MulticastConfiguration {
        void host(String host);

        void port(int port);

        void netInterface(String netInterface);

        void receiveBufferSize(int size);

        void sendBufferSize(int size);

        void packetsPerSecond(int pps);

        void spinLoopMicros(int micros);

        void threadParkMicros(int micros);

        void retransmitPacketsHistory(int seconds);

        void datagramSize(int size);

        void preferBatchingToLatency(boolean batching);

        void ttl(int ttl);

        void sendRetries(int retries);
    }

    interface UnicastConfiguration {
        void receiveBufferSize(int size);

        void sendBufferSize(int size);

        void pingTimeoutMillis(int pingTimeout);

        void minReceiveThreads(int threads);

        void maxReceiveThreads(int threads);

        void receiveQueueMaxSize(int size);

        void maxReadBatchMessages(int size);

        void retransmitIntervalMillis(int millis);

        void maxRetransmitTimeMillis(int millis);

        void maxStableAttempts(int attempts);
    }

    enum CommandsXmitTransport {
        UNICAST, MULTICAST
    }
}
