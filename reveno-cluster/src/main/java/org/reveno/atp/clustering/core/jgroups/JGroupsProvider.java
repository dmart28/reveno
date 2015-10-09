package org.reveno.atp.clustering.core.jgroups;

import org.jgroups.JChannel;
import org.reveno.atp.clustering.api.Address;
import org.reveno.atp.clustering.api.Cluster;
import org.reveno.atp.clustering.api.ClusterBuffer;
import org.reveno.atp.clustering.api.InetAddress;
import org.reveno.atp.clustering.core.RevenoClusterConfiguration;
import org.reveno.atp.clustering.core.buffer.ClusterProvider;
import org.reveno.atp.utils.Exceptions;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class JGroupsProvider implements ClusterProvider {
    protected static final String CLUSTER_NAME = "rvno_jg";

    public void initialize(RevenoClusterConfiguration config) {
        this.config = config;
        System.setProperty("jgroups.tcp.bind_addr", ((InetAddress) config.currentNodeAddress()).getHost());
        System.setProperty("jgroups.tcp.bind_port", Integer.toString(((InetAddress) config.currentNodeAddress()).getPort()));
        System.setProperty("jgroups.tcpping.initial_hosts", makeInitialHostsString(config.clusterNodeAddresses()));
        System.setProperty("jgroups.rsvp.timeout", Long.toString(config.revenoTimeouts().ackTimeout()));
        System.setProperty("jgroups.auth.token", Optional.ofNullable(config.authToken()).orElse(""));

        try {
            if (configFilePath.startsWith("classpath:/")) {
                channel = new JChannel(getClass().getClassLoader().getResourceAsStream(configFilePath.replace("classpath:/", "")));
            } else {
                channel = new JChannel(new File(configFilePath));
            }
            channel.setReceiver(new JChannelReceiver());

            channel.connect(CLUSTER_NAME);
        } catch (Exception e) {
            throw Exceptions.runtime(e);
        }
        isInitialized = true;
    }

    @Override
    public Cluster retrieveCluster() {
        checkInitialized();
        return new JGroupsCluster(config, channel);
    }

    @Override
    public ClusterBuffer retrieveBuffer() {
        checkInitialized();
        return new JGroupsBuffer(config, channel);
    }

    protected String makeInitialHostsString(List<Address> addresses) {
        StringBuilder sb = new StringBuilder();
        addresses.stream().map(a -> (InetAddress)a).forEach(a -> sb.append(a.getHost())
                .append("[").append(a.getPort()).append("]").append(","));
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    protected void checkInitialized() {
        if (!isInitialized)
            throw new IllegalStateException("JGroups provider must be initialized first.");
    }

    public JGroupsProvider(String configFilePath) {
        this.configFilePath = configFilePath;
    }

    protected RevenoClusterConfiguration config;
    protected JChannel channel;
    protected String configFilePath;
    protected boolean isInitialized = false;
}
