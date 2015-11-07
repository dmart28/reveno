package org.reveno.atp.clustering.core.providers;

import org.reveno.atp.clustering.api.ClusterBuffer;
import org.reveno.atp.clustering.core.jgroups.JGroupsBuffer;
import java.util.Properties;

public class UnicastAllProvider extends JGroupsClusterProvider {

    @Override
    ClusterBuffer createBuffer() {
        return new JGroupsBuffer(config, channel);
    }

    @Override
    void setProperties(Properties properties) {
        properties.setProperty("max.stable.attempts", Integer.toString(config.revenoUnicast().maxStableAttempts()));
        properties.setProperty("max.retransmit.time", Integer.toString(config.revenoUnicast().maxRetransmitTimeMillis()));
        properties.setProperty("retransmit.interval", Integer.toString(config.revenoUnicast().retransmitIntervalMillis()));
        properties.setProperty("nodes.ping.timeout", Integer.toString(config.revenoUnicast().pingTimeoutMillis()));
        properties.setProperty("jgroups.threads.min", Integer.toString(config.revenoUnicast().minReceiveThreads()));
        properties.setProperty("jgroups.threads.max", Integer.toString(config.revenoUnicast().maxReceiveThreads()));
        if (config.revenoUnicast().receiveQueueMaxSize() == 0) {
            properties.setProperty("jgroups.threads.queue", "false");
        } else {
            properties.setProperty("jgroups.threads.queue", "true");
            properties.setProperty("jgroups.threads.queue.size", Integer.toString(config.revenoUnicast().receiveQueueMaxSize()));
        }
        properties.setProperty("max.read.batch.size", Integer.toString(config.revenoUnicast().maxReadBatchMessages()));
        properties.setProperty("tcp.send_buf_size", Integer.toString(config.revenoUnicast().sendBufferSize()));
        properties.setProperty("tcp.recv_buf_size", Integer.toString(config.revenoUnicast().receiveBufferSize()));
    }

    public UnicastAllProvider() {
        this.jGroupsConfigFile = "classpath:/tcp.xml";
    }

    public UnicastAllProvider(String jGroupsConfigFile) {
        this.jGroupsConfigFile = jGroupsConfigFile;
    }

}
