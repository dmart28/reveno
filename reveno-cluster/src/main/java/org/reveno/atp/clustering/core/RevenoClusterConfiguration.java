package org.reveno.atp.clustering.core;

import org.reveno.atp.clustering.api.Address;
import org.reveno.atp.clustering.api.ClusterConfiguration;
import org.reveno.atp.clustering.api.InetAddress;
import org.reveno.atp.clustering.api.SyncMode;

import java.util.Collections;
import java.util.List;

public class RevenoClusterConfiguration implements ClusterConfiguration {

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
    public TimeoutsConfiguration timeouts() {
        return timeouts;
    }

    @Override
    public SyncConfiguration sync() {
        return sync;
    }
    public RevenoSyncConfiguration revenoSync() {
        return sync;
    }

    public RevenoTimeoutsConfiguration revenoTimeouts() {
        return timeouts;
    }

    @Override
    public void authToken(String authToken) {
        this.authToken = authToken;
    }
    public String authToken() {
        return authToken;
    }

    @Override
    public void priority(int priority) {
        this.priority = priority;
    }
    public int priority() {
        return priority;
    }


    protected Address nodeAddress;
    protected List<Address> nodeAddresses;
    protected int priority = 1;
    protected String authToken;
    protected RevenoTimeoutsConfiguration timeouts = new RevenoTimeoutsConfiguration();
    protected RevenoSyncConfiguration sync = new RevenoSyncConfiguration();

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

        protected long voteTimeout = 1000;
        protected long syncTimeout = 1000;
        protected long ackTimeout = 1000;
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
        protected boolean waitAllNodesSync = false;
    }
}
