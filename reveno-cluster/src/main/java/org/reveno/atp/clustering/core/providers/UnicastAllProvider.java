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
