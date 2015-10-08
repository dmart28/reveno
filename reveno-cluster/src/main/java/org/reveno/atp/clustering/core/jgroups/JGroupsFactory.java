package org.reveno.atp.clustering.core.jgroups;

import org.reveno.atp.clustering.api.Cluster;
import org.reveno.atp.clustering.api.ClusterBuffer;
import org.reveno.atp.clustering.core.RevenoClusterConfiguration;
import org.reveno.atp.clustering.core.buffer.ClusterFactory;

public class JGroupsFactory implements ClusterFactory {

    @Override
    public Cluster createCluster(RevenoClusterConfiguration config) {
        return new JGroupsCluster(config, configFilePath);
    }

    @Override
    public ClusterBuffer createBuffer(RevenoClusterConfiguration config) {
        return null;
    }

    public JGroupsFactory(String configFilePath) {
        this.configFilePath = configFilePath;
    }

    protected String configFilePath;
}
