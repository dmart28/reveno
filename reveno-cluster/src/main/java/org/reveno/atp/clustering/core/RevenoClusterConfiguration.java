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


package org.reveno.atp.clustering.core;

import org.reveno.atp.clustering.api.Address;
import org.reveno.atp.clustering.api.ClusterConfiguration;
import org.reveno.atp.clustering.api.InetAddress;
import org.reveno.atp.clustering.api.SyncMode;
import org.reveno.atp.utils.MeasureUtils;

import java.util.Collections;
import java.util.List;

public class RevenoClusterConfiguration implements ClusterConfiguration {

    protected static final long DEFAULT_TIMEOUT = MeasureUtils.sec(5);

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
        public void voteTimeoutNanos(long timeout) {
            this.voteTimeout = timeout;
        }
        public long voteTimeoutNanos() {
            return voteTimeout;
        }

        @Override
        public void syncTimeoutNanos(long timeout) {
            this.syncTimeout = timeout;
        }
        public long syncTimeoutNanos() {
            return syncTimeout;
        }

        @Override
        public void ackTimeoutNanos(long timeout) {
            this.ackTimeout = timeout;
        }
        public long ackTimeoutNanos() {
            return ackTimeout;
        }

        @Override
        public void barrierTimeoutNanos(long timeout) {
            this.barrierTimeout = timeout;
        }
        public long barrierTimeoutNanos() {
            return barrierTimeout;
        }

        @Override
        public void syncBarrierTimeoutNanos(long timeout) {
            this.syncBarrierTimeoutNanos = timeout;
        }
        public long syncBarrierTimeoutNanos() {
            return syncBarrierTimeoutNanos;
        }

        protected long voteTimeout = DEFAULT_TIMEOUT;
        protected long syncTimeout = DEFAULT_TIMEOUT;
        protected long ackTimeout = DEFAULT_TIMEOUT;
        protected long barrierTimeout = MeasureUtils.sec(10);
        protected long syncBarrierTimeoutNanos = MeasureUtils.sec(3 * 60);
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
        protected int receiveBufferSize = 1024 * 1024;
        protected int sendBufferSize = 1024 * 1024;
        protected int packetsPerSecond = 3_000;
        protected int spinLoopMicros = 0;
        protected int threadParkMicros = 20;
        protected int retransmitPacketsHistory = 9_000;
        protected int datagramSize = 1000;
        protected int ttl = 8;
        protected boolean preferBatchingToLatency = true;
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
