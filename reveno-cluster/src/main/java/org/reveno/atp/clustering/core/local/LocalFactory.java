package org.reveno.atp.clustering.core.local;

import org.reveno.atp.clustering.api.Cluster;
import org.reveno.atp.clustering.api.ClusterBuffer;
import org.reveno.atp.clustering.core.RevenoClusterConfiguration;
import org.reveno.atp.clustering.core.buffer.ClusterFactory;

public class LocalFactory implements ClusterFactory {

    @Override
    public Cluster createCluster(RevenoClusterConfiguration config) {
        return null;
    }

    @Override
    public ClusterBuffer createBuffer(RevenoClusterConfiguration config) {
        return new LocalBuffer();
    }
}
