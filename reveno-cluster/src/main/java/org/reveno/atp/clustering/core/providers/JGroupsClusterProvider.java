package org.reveno.atp.clustering.core.providers;

import org.jgroups.JChannel;
import org.reveno.atp.clustering.api.Address;
import org.reveno.atp.clustering.api.Cluster;
import org.reveno.atp.clustering.api.ClusterBuffer;
import org.reveno.atp.clustering.api.InetAddress;
import org.reveno.atp.clustering.core.RevenoClusterConfiguration;
import org.reveno.atp.clustering.core.buffer.ClusterProvider;
import org.reveno.atp.clustering.core.jgroups.JChannelReceiver;
import org.reveno.atp.clustering.core.jgroups.JGroupsCluster;
import org.reveno.atp.clustering.util.ResourceLoader;
import org.reveno.atp.utils.Exceptions;
import org.w3c.dom.Element;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

public abstract class JGroupsClusterProvider implements ClusterProvider {
    public static final String CLUSTER_NAME = "rvno_jg";

    abstract ClusterBuffer createBuffer();

    abstract void setProperties(Properties properties);

    @Override
    public void initialize(RevenoClusterConfiguration config) {
        this.config = config;
        Properties props = new Properties();
        props.put("jgroups.tcp.bind_addr", ((InetAddress) config.currentNodeAddress()).getHost());
        props.put("jgroups.tcp.bind_port", Integer.toString(((InetAddress) config.currentNodeAddress()).getPort()));
        props.put("jgroups.tcpping.initial_hosts", makeInitialHostsString(config.clusterNodeAddresses()));
        props.put("jgroups.auth.token", Optional.ofNullable(config.authToken()).orElse(""));
        setProperties(props);

        try {
            String protocol;
            if (jGroupsConfigFile.startsWith("classpath:/")) {
                protocol = ResourceLoader.loadResource(getClass().getClassLoader()
                        .getResourceAsStream(jGroupsConfigFile.replace("classpath:/", "")), props);
            } else {
                protocol = ResourceLoader.loadResource(new File(jGroupsConfigFile), props);
            }
            Element xml = ResourceLoader.loadXMLFromString(protocol).getDocumentElement();
            channel = new JChannel(xml);
            channel.setReceiver(new JChannelReceiver());
            channel.setDiscardOwnMessages(true);
            cluster = new JGroupsCluster(config, channel);
            buffer = createBuffer();
        } catch (Exception e) {
            throw Exceptions.runtime(e);
        }
        isInitialized = true;
    }

    @Override
    public Cluster retrieveCluster() {
        checkInitialized();
        return cluster;
    }

    @Override
    public ClusterBuffer retrieveBuffer() {
        checkInitialized();
        return buffer;
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
            throw new IllegalStateException("Provider must be initialized first.");
    }

    protected String jGroupsConfigFile;
    protected RevenoClusterConfiguration config;
    protected JGroupsCluster cluster;
    protected ClusterBuffer buffer;
    protected JChannel channel;
    protected boolean isInitialized = false;
}