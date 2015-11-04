package org.reveno.atp.clustering.core.fastcast;

import org.reveno.atp.clustering.api.Address;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class FastCastConfiguration {

    protected Address currentNode;
    public Address getCurrentNode() {
        return currentNode;
    }
    public FastCastConfiguration setCurrentNode(Address currentNode) {
        this.currentNode = currentNode;
        return this;
    }

    protected List<Address> nodeAddresses;
    public List<Address> getNodeAddresses() {
        return nodeAddresses;
    }
    public FastCastConfiguration setNodeAddresses(List<Address> nodeAddresses) {
        this.nodeAddresses = nodeAddresses;
        return this;
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
