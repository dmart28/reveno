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
import org.reveno.atp.clustering.core.fastcast.FastCastBuffer;
import org.reveno.atp.clustering.core.fastcast.FastCastConfiguration;

import java.util.Properties;

public class MulticastAllProvider extends JGroupsClusterProvider {

    @Override
    ClusterBuffer createBuffer() {
        if (fcConfig == null) {
            fcConfig = new FastCastConfiguration();
            fcConfig.datagramSize(config.revenoMulticast().datagramSize());
            fcConfig.mcastHost(config.revenoMulticast().host());
            fcConfig.mcastPort(config.revenoMulticast().port());
            fcConfig.networkInterface(config.revenoMulticast().netInterface());
            fcConfig.packetsPerSecond(config.revenoMulticast().packetsPerSecond());
            fcConfig.retransmissionPacketHistory(config.revenoMulticast().retransmitPacketsHistory());
            fcConfig.setNodeAddresses(config.nodesAddresses());
            fcConfig.socketConfiguration().socketReceiveBufferSize(config.revenoMulticast().receiveBufferSize());
            fcConfig.socketConfiguration().socketSendBufferSize(config.revenoMulticast().sendBufferSize());
            fcConfig.socketConfiguration().ttl(config.revenoMulticast().ttl());
            fcConfig.setCurrentNode(config.currentNodeAddress());
            fcConfig.spinLoopMicros(config.revenoMulticast().spinLoopMicros());
            fcConfig.threadParkMicros(config.revenoMulticast().threadParkMicros());
            fcConfig.topicName("rvntopic");
            fcConfig.transportName("default");
            fcConfig.alwaysFlush(!config.revenoMulticast().preferBatchingToLatency());
            fcConfig.sendRetries(config.revenoMulticast().sendRetries());
        }
        return new FastCastBuffer(fcConfig);
    }

    @Override
    void setProperties(Properties properties) {
    }

    public MulticastAllProvider() {
        jGroupsConfigFile = "classpath:/tcp.xml";
    }

    public MulticastAllProvider(FastCastConfiguration fcConfig) {
        this.fcConfig = fcConfig;
        jGroupsConfigFile = "classpath:/tcp.xml";
    }

    public MulticastAllProvider(FastCastConfiguration fcConfig, String jGroupsConfigFile) {
        this.fcConfig = fcConfig;
        this.jGroupsConfigFile = jGroupsConfigFile;
    }

    public MulticastAllProvider(String jGroupsConfigFile) {
        this.jGroupsConfigFile = jGroupsConfigFile;
    }

    protected FastCastConfiguration fcConfig;

}
