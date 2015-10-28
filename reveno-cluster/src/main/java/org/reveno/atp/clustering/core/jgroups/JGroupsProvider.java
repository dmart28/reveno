package org.reveno.atp.clustering.core.jgroups;

import org.jgroups.JChannel;
import org.reveno.atp.clustering.api.Address;
import org.reveno.atp.clustering.api.Cluster;
import org.reveno.atp.clustering.api.ClusterBuffer;
import org.reveno.atp.clustering.api.InetAddress;
import org.reveno.atp.clustering.core.RevenoClusterConfiguration;
import org.reveno.atp.clustering.core.buffer.ClusterProvider;
import org.reveno.atp.clustering.util.ResourceLoader;
import org.reveno.atp.utils.Exceptions;
import org.w3c.dom.Element;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

public class JGroupsProvider implements ClusterProvider {
    protected static final String CLUSTER_NAME = "rvno_jg";

    public void initialize(RevenoClusterConfiguration config) {
        this.config = config;
        Properties props = new Properties();
        props.put("jgroups.tcp.bind_addr", ((InetAddress) config.currentNodeAddress()).getHost());
        props.put("jgroups.tcp.bind_port", Integer.toString(((InetAddress) config.currentNodeAddress()).getPort()));
        props.put("jgroups.tcpping.initial_hosts", makeInitialHostsString(config.clusterNodeAddresses()));
        props.put("jgroups.rsvp.timeout", Long.toString(config.revenoTimeouts().ackTimeout()));
        props.put("jgroups.auth.token", Optional.ofNullable(config.authToken()).orElse(""));

        try {
            String protocol;
            if (configFilePath.startsWith("classpath:/")) {
                protocol = ResourceLoader.loadResource(getClass().getClassLoader()
                                .getResourceAsStream(configFilePath.replace("classpath:/", "")), props);
            } else {
                protocol = ResourceLoader.loadResource(new File(configFilePath), props);
            }
            Element xml = ResourceLoader.loadXMLFromString(protocol).getDocumentElement();
            channel = new JChannel(xml);
            channel.setReceiver(new JChannelReceiver());
            jcluster = new JGroupsCluster(config, channel);
            jbuffer = new JGroupsBuffer(config, channel);
        } catch (Exception e) {
            throw Exceptions.runtime(e);
        }
        isInitialized = true;
    }

    @Override
    public Cluster retrieveCluster() {
        checkInitialized();
        return jcluster;
    }

    @Override
    public ClusterBuffer retrieveBuffer() {
        checkInitialized();
        return jbuffer;
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
    protected JGroupsCluster jcluster;
    protected JGroupsBuffer jbuffer;
    protected boolean isInitialized = false;
}
