package org.reveno.atp.clustering.core;

import org.reveno.atp.clustering.api.Address;
import org.reveno.atp.clustering.api.ClusterConfiguration;

import java.util.Collections;
import java.util.List;

public class RevenoClusterConfiguration implements ClusterConfiguration {

    @Override
    public void currentNodeAddress(Address nodeAddress) {
        this.nodeAddress = nodeAddress;
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
    public RevenoTimeoutsConfiguration revenoTimeouts() {
        return timeouts;
    }

    @Override
    public void syncRetries(int count) {
        this.syncRetries = count;
    }
    public int syncRetries() {
        return syncRetries;
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

    @Override
    public void syncMode(SyncMode mode) {
        this.syncMode = mode;
    }

    @Override
    public void syncThreadPoolSize(int threads) {
        this.syncThreadPoolSize = threads;
    }
    public int syncThreadPoolSize() {
        return syncThreadPoolSize;
    }

    public SyncMode syncMode() {
        return syncMode;
    }

    public int priority() {
        return priority;
    }


    protected Address nodeAddress;
    protected List<Address> nodeAddresses;
    protected int syncRetries = 100;
    protected int priority = 1;
    protected String authToken;
    protected SyncMode syncMode = SyncMode.SNAPSHOT;
    protected int syncThreadPoolSize = 10;
    protected RevenoTimeoutsConfiguration timeouts = new RevenoTimeoutsConfiguration();

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
}
