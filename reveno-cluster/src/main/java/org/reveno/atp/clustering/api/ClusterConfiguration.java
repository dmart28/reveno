package org.reveno.atp.clustering.api;

import java.util.List;

public interface ClusterConfiguration {

    void currentNodeAddress(Address nodeAddress);

    void clusterNodeAddresses(List<Address> nodeAddresses);

    TimeoutsConfiguration timeouts();

    void syncRetries(int count);

    void authToken(String authToken);

    void priority(int priority);


    interface TimeoutsConfiguration {
        void voteTimeout(long timeout);

        void syncTimeout(long timeout);

        void ackTimeout(long timeout);
    }

}
