package org.reveno.atp.clustering.core.buffer;

import org.reveno.atp.clustering.api.Cluster;
import org.reveno.atp.clustering.api.ClusterBuffer;
import org.reveno.atp.clustering.core.RevenoClusterConfiguration;

public interface ClusterProvider {

    void initialize(RevenoClusterConfiguration config);

    Cluster retrieveCluster();

    ClusterBuffer retrieveBuffer();

}
