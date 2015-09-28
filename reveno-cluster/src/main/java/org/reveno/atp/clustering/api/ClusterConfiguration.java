package org.reveno.atp.clustering.api;

import java.util.List;

public interface ClusterConfiguration {

    void currentNodeAddress(Address nodeAddress);

    void clusterNodeAddresses(List<Address> nodeAddresses);

    TimeoutsConfiguration timeouts();

    void syncRetries(int count);

    void authToken(String authToken);

    void priority(int priority);

    void syncMode(SyncMode mode);

    void syncThreadPoolSize(int threads);


    interface TimeoutsConfiguration {
        void voteTimeout(long timeout);

        void syncTimeout(long timeout);

        void ackTimeout(long timeout);
    }

    enum SyncMode {
        SNAPSHOT((byte) 1), JOURNALS((byte) 2);

        protected byte type;

        public byte getType() {
            return type;
        }

        SyncMode(byte type) {
            this.type = type;
        }
    }

}
