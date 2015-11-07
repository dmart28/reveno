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
        void voteTimeout(long timeout);

        void syncTimeout(long timeout);

        void ackTimeout(long timeout);
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

        void ttl(int ttl);
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
