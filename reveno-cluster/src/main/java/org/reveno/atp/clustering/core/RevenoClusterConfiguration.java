package org.reveno.atp.clustering.core;

import org.reveno.atp.clustering.api.Address;
import org.reveno.atp.clustering.api.ClusterConfiguration;
import org.reveno.atp.clustering.api.InetAddress;
import org.reveno.atp.clustering.api.SyncMode;

import java.util.Collections;
import java.util.List;

public class RevenoClusterConfiguration implements ClusterConfiguration {

    protected static final int DEFAULT_TIMEOUT = 2000;

    @Override
    public void currentNodeAddress(Address nodeAddress) {
        this.nodeAddress = nodeAddress;
        if (sync.port == 0 && nodeAddress instanceof InetAddress) {
            sync.port = ((InetAddress) nodeAddress).getPort() + 1;
        }
    }
    public Address currentNodeAddress() {
        return nodeAddress;
    }

    @Override
    public void clusterNodeAddresses(List<Address> nodeAddresses) {
        this.nodeAddresses = nodeAddresses;
    }
    public List<Address> clusterNodeAddresses() {
        return Collections.unmodifiableList(nodeAddresses);
    }

    @Override
    public void commandsXmitTransport(CommandsXmitTransport transport) {
        this.transport = transport;
    }
    public CommandsXmitTransport commandsXmitTransport() {
        return transport;
    }

    @Override
    public MulticastConfiguration multicast() {
        return multicast;
    }
    public RevenoMulticastConfiguration revenoMulticast() {
        return multicast;
    }

    @Override
    public UnicastConfiguration unicast() {
        return unicast;
    }
    public RevenoUnicastConfiguration revenoUnicast() {
        return unicast;
    }

    @Override
    public TimeoutsConfiguration electionTimeouts() {
        return timeouts;
    }
    public RevenoTimeoutsConfiguration revenoElectionTimeouts() {
        return timeouts;
    }

    @Override
    public SyncConfiguration dataSync() {
        return sync;
    }
    public RevenoSyncConfiguration revenoDataSync() {
        return sync;
    }

    @Override
    public void authToken(String authToken) {
        this.authToken = authToken;
    }
    public String authToken() {
        return authToken;
    }

    @Override
    public void priorityInCluster(int priority) {
        this.priority = priority;
    }
    public int priorityInCluster() {
        return priority;
    }


    protected Address nodeAddress;
    protected List<Address> nodeAddresses;
    protected int priority = 1;
    protected String authToken;
    protected CommandsXmitTransport transport = CommandsXmitTransport.UNICAST;
    protected RevenoTimeoutsConfiguration timeouts = new RevenoTimeoutsConfiguration();
    protected RevenoSyncConfiguration sync = new RevenoSyncConfiguration();
    protected RevenoMulticastConfiguration multicast = new RevenoMulticastConfiguration();
    protected RevenoUnicastConfiguration unicast = new RevenoUnicastConfiguration();

    public static class RevenoTimeoutsConfiguration implements TimeoutsConfiguration {
        @Override
        public void voteTimeout(long timeout) {
            this.voteTimeout = timeout;
        }
        public long voteTimeout() {
            return voteTimeout;
        }

        @Override
        public void syncTimeout(long timeout) {
            this.syncTimeout = timeout;
        }
        public long syncTimeout() {
            return syncTimeout;
        }

        @Override
        public void ackTimeout(long timeout) {
            this.ackTimeout = timeout;
        }
        public long ackTimeout() {
            return ackTimeout;
        }

        protected long voteTimeout = DEFAULT_TIMEOUT;
        protected long syncTimeout = DEFAULT_TIMEOUT;
        protected long ackTimeout = DEFAULT_TIMEOUT;
    }

    public static class RevenoSyncConfiguration implements SyncConfiguration {

        @Override
        public void mode(SyncMode mode) {
            this.mode = mode;
        }
        public SyncMode mode() {
            return mode;
        }

        @Override
        public void port(int port) {
            this.port = port;
        }
        public int port() {
            return port;
        }

        @Override
        public void waitAllNodesSync(boolean wait) {
            this.waitAllNodesSync = wait;
        }
        public boolean waitAllNodesSync() {
            return waitAllNodesSync;
        }

        @Override
        public void retries(int count) {
            this.retries = count;
        }
        public int retries() {
            return retries;
        }

        @Override
        public void threadPoolSize(int threads) {
            this.threadPoolSize = threads;
        }
        public int threadPoolSize() {
            return threadPoolSize;
        }

        protected int threadPoolSize = 10;
        protected int retries = 100;
        protected SyncMode mode = SyncMode.SNAPSHOT;
        protected int port;
        protected boolean waitAllNodesSync = true;
    }

    public static class RevenoMulticastConfiguration implements MulticastConfiguration {

        @Override
        public void host(String host) {
            this.host = host;
        }
        public String host() {
            return host;
        }

        @Override
        public void port(int port) {
            this.port = port;
        }
        public int port() {
            return port;
        }

        @Override
        public void netInterface(String netInterface) {
            this.netInterface = netInterface;
        }
        public String netInterface() {
            return netInterface;
        }

        @Override
        public void receiveBufferSize(int size) {
            this.receiveBufferSize = size;
        }
        public int receiveBufferSize() {
            return receiveBufferSize;
        }

        @Override
        public void sendBufferSize(int size) {
            this.sendBufferSize = size;
        }
        public int sendBufferSize() {
            return sendBufferSize;
        }

        @Override
        public void packetsPerSecond(int pps) {
            this.packetsPerSecond = pps;
        }
        public int packetsPerSecond() {
            return packetsPerSecond;
        }

        @Override
        public void spinLoopMicros(int micros) {
            this.spinLoopMicros = micros;
        }
        public int spinLoopMicros() {
            return spinLoopMicros;
        }

        @Override
        public void threadParkMicros(int micros) {
            this.threadParkMicros = micros;
        }
        public int threadParkMicros() {
            return threadParkMicros;
        }

        @Override
        public void retransmitPacketsHistory(int packets) {
            this.retransmitPacketsHistory = packets;
        }
        public int retransmitPacketsHistory() {
            return retransmitPacketsHistory;
        }

        @Override
        public void datagramSize(int size) {
            this.datagramSize = size;
        }
        public int datagramSize() {
            return datagramSize;
        }

        @Override
        public void preferBatchingToLatency(boolean batching) {
            this.preferBatchingToLatency = batching;
        }
        public boolean preferBatchingToLatency() {
            return preferBatchingToLatency;
        }

        @Override
        public void ttl(int ttl) {
            this.ttl = ttl;
        }
        public int ttl() {
            return ttl;
        }

        @Override
        public void sendRetries(int retries) {
            this.sendRetries = retries;
        }
        public int sendRetries() {
            return sendRetries;
        }

        protected String host;
        protected int port;
        protected String netInterface = "127.0.0.1";
        protected int receiveBufferSize = 3 * 1024 * 1024;
        protected int sendBufferSize = 1024 * 1024;
        protected int packetsPerSecond = 5_000;
        protected int spinLoopMicros = 0;
        protected int threadParkMicros = 20;
        protected int retransmitPacketsHistory = 80_000;
        protected int datagramSize = 1000;
        protected int ttl = 8;
        protected boolean preferBatchingToLatency = false;
        protected int sendRetries = 15;
    }

    public static class RevenoUnicastConfiguration implements UnicastConfiguration {

        @Override
        public void receiveBufferSize(int size) {
            this.receiveBufferSize = size;
        }
        public int receiveBufferSize() {
            return receiveBufferSize;
        }

        @Override
        public void sendBufferSize(int size) {
            this.sendBufferSize = size;
        }
        public int sendBufferSize() {
            return sendBufferSize;
        }

        @Override
        public void pingTimeoutMillis(int pingTimeout) {
            this.pingTimeoutMls = pingTimeout;
        }
        public int pingTimeoutMillis() {
            return pingTimeoutMls;
        }

        @Override
        public void minReceiveThreads(int threads) {
            this.minReceiveThreads = threads;
        }
        public int minReceiveThreads() {
            return minReceiveThreads;
        }

        @Override
        public void maxReceiveThreads(int threads) {
            this.maxReceiveThreads = threads;
        }
        public int maxReceiveThreads() {
            return maxReceiveThreads;
        }

        @Override
        public void receiveQueueMaxSize(int size) {
            this.receiveQueueMaxSize = size;
        }
        public int receiveQueueMaxSize() {
            return receiveQueueMaxSize;
        }

        @Override
        public void maxReadBatchMessages(int size) {
            this.maxReadBatchMessages = size;
        }
        public int maxReadBatchMessages() {
            return maxReadBatchMessages;
        }

        @Override
        public void retransmitIntervalMillis(int millis) {
            this.retransmitIntervalMillis = millis;
        }
        public int retransmitIntervalMillis() {
            return retransmitIntervalMillis;
        }

        @Override
        public void maxRetransmitTimeMillis(int millis) {
            this.maxRetransmitTimeMillis = millis;
        }
        public int maxRetransmitTimeMillis() {
            return maxRetransmitTimeMillis;
        }

        @Override
        public void maxStableAttempts(int attempts) {
            this.maxStableAttempts = attempts;
        }
        public int maxStableAttempts() {
            return maxStableAttempts;
        }

        protected int receiveBufferSize = 5 * 1024 * 1024;
        protected int sendBufferSize = 1024 * 1024;
        protected int pingTimeoutMls = 5000;
        protected int maxReceiveThreads = 1;
        protected int minReceiveThreads = 1;
        protected int receiveQueueMaxSize = 0;
        protected int maxReadBatchMessages = 10;
        protected int retransmitIntervalMillis = 1000;
        protected int maxRetransmitTimeMillis = 1500;
        protected int maxStableAttempts = 3;
    }
}
