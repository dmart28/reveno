package org.reveno.atp.clustering.core.fastcast;

import org.nustaq.fastcast.util.FCUtils;

import java.io.File;
import java.util.Optional;

public class FastCastConfiguration {

    protected String nodeId = FCUtils.createNodeId("");
    public FastCastConfiguration nodeId(String nodeId) {
        this.nodeId = nodeId;
        return this;
    }
    public String nodeId() {
        return nodeId;
    }

    protected String topicName = "rvntopic";
    public FastCastConfiguration topicName(String topicName) {
        this.topicName = topicName;
        return this;
    }
    public String topicName() {
        return topicName;
    }

    protected String transportName = "default";
    public FastCastConfiguration transportName(String transportName) {
        this.transportName = transportName;
        return this;
    }
    public String transportName() {
        return transportName;
    }

    protected String networkInterface;
    public FastCastConfiguration networkInterface(String networkInterface) {
        this.networkInterface = networkInterface;
        return this;
    }
    public String networkInterface() {
        return networkInterface;
    }

    protected String mcastHost;
    public FastCastConfiguration mcastHost(String mcastHost) {
        this.mcastHost = mcastHost;
        return this;
    }
    public String mcastHost() {
        return mcastHost;
    }

    protected int mcastPort;
    public FastCastConfiguration mcastPort(int mcastPort) {
        this.mcastPort = mcastPort;
        return this;
    }
    public int mcastPort() {
        return mcastPort;
    }

    protected File configFile;
    public FastCastConfiguration configFile(File configFile) {
        this.configFile = configFile;
        return this;
    }
    public Optional<File> configFile() {
        return Optional.ofNullable(configFile);
    }

}
