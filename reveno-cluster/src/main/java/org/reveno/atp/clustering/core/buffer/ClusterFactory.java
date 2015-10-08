package org.reveno.atp.clustering.core.buffer;

import org.reveno.atp.clustering.api.Cluster;
import org.reveno.atp.clustering.api.ClusterBuffer;
import org.reveno.atp.clustering.core.RevenoClusterConfiguration;

public interface ClusterFactory {

    Cluster createCluster(RevenoClusterConfiguration config);

    ClusterBuffer createBuffer(RevenoClusterConfiguration config);

}
