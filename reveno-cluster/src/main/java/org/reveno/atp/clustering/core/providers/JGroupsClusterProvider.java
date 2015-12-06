/**
 *  Copyright (c) 2015 The original author or authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */


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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * Abstract provider where {@link Cluster} implementation is always used as
 * JGroups, and {@link ClusterBuffer} may vary.
 */
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
        props.put("jgroups.tcpping.initial_hosts", makeInitialHostsString(config.nodesAddresses()));
        props.put("jgroups.auth.token", Optional.ofNullable(config.authToken()).orElse(""));
        setProperties(props);

        try {
            String protocol;
            if (jGroupsConfigFile.startsWith("classpath:/")) {
                protocol = ResourceLoader.loadResource(new ByteArrayInputStream(DEFAULT_CONFIG.getBytes("UTF-8")), props);
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

    protected static final String DEFAULT_CONFIG = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"urn:org:jgroups\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:schemaLocation=\"urn:org:jgroups http://www.jgroups.org/schema/JGroups-3.4.xsd\"><TCP_NIO2 bind_addr=\"" +
            "${jgroups.tcp.bind_addr}\" bind_port=\"${jgroups.tcp.bind_port}\" port_range=\"0\" recv_buf_size=\"" +
            "${tcp.recv_buf_size:5M}\" send_buf_size=\"${tcp.send_buf_size:5M}\" max_bundle_size=\"4k\" max_bundle_timeout=\"" +
            "1\" max_read_batch_size=\"${max.read.batch.size:10}\" sock_conn_timeout=\"300\" timer_type=\"new3\" timer.min_threads=\"4\"" +
            " timer.max_threads=\"10\" timer.keep_alive_time=\"3000\" timer.queue_max_size=\"3000\" thread_pool.enabled=\"true\"" +
            " thread_pool.min_threads=\"${jgroups.threads.min:5}\" thread_pool.max_threads=\"${jgroups.threads.max:60}\"" +
            " thread_pool.keep_alive_time=\"5000\" thread_pool.queue_enabled=\"${jgroups.threads.queue:false}\" " +
            "thread_pool.queue_max_size=\"${jgroups.threads.queue.size:500000}\" thread_pool.rejection_policy=\"Run\"" +
            " oob_thread_pool.enabled=\"true\" oob_thread_pool.min_threads=\"5\" oob_thread_pool.max_threads=\"15\" " +
            "oob_thread_pool.keep_alive_time=\"50000\" oob_thread_pool.queue_enabled=\"true\" oob_thread_pool.queue_max_size=\"10000\"" +
            " oob_thread_pool.rejection_policy=\"Run\" /><TCPPING timeout=\"${nodes.ping.timeout:2000}\" initial_hosts=\"" +
            "${jgroups.tcpping.initial_hosts}\" port_range=\"0\" /><MERGE3 min_interval=\"1000\" max_interval=\"3000\"" +
            " /><FD_SOCK /><VERIFY_SUSPECT timeout=\"1500\" /><BARRIER /><pbcast.NAKACK2 use_mcast_xmit=\"false\"" +
            " discard_delivered_msgs=\"true\" /><UNICAST3 xmit_interval=\"${retransmit.interval:100}\" max_retransmit_time=\"" +
            "${max.retransmit.time:150}\" /><SCOPE /><pbcast.STABLE stability_delay=\"1000\" desired_avg_gossip=\"50000\"" +
            " max_bytes=\"4M\" /><pbcast.GMS print_local_addr=\"true\" join_timeout=\"3000\" view_bundling=\"true\" " +
            "/><pbcast.STATE_TRANSFER /><pbcast.FLUSH timeout=\"0\" /></config>";
}