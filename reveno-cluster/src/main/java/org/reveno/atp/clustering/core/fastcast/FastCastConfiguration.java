package org.reveno.atp.clustering.core.fastcast;

import org.reveno.atp.clustering.api.Address;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class FastCastConfiguration {

    private Address currentNode;
    public Address getCurrentNode() {
        return currentNode;
    }
    public FastCastConfiguration setCurrentNode(Address currentNode) {
        this.currentNode = currentNode;
        return this;
    }

    private List<Address> nodeAddresses;
    public List<Address> getNodeAddresses() {
        return nodeAddresses;
    }
    public FastCastConfiguration setNodeAddresses(List<Address> nodeAddresses) {
        this.nodeAddresses = nodeAddresses;
        return this;
    }

    private String topicName = "rvntopic";
    public FastCastConfiguration topicName(String topicName) {
        this.topicName = topicName;
        return this;
    }
    public String topicName() {
        return topicName;
    }

    private String transportName = "default";
    public FastCastConfiguration transportName(String transportName) {
        this.transportName = transportName;
        return this;
    }
    public String transportName() {
        return transportName;
    }

    private String networkInterface;
    public FastCastConfiguration networkInterface(String networkInterface) {
        this.networkInterface = networkInterface;
        return this;
    }
    public String networkInterface() {
        return networkInterface;
    }

    private String mcastHost;
    public FastCastConfiguration mcastHost(String mcastHost) {
        this.mcastHost = mcastHost;
        return this;
    }
    public String mcastHost() {
        return mcastHost;
    }

    private int mcastPort;
    public FastCastConfiguration mcastPort(int mcastPort) {
        this.mcastPort = mcastPort;
        return this;
    }
    public int mcastPort() {
        return mcastPort;
    }

    private int datagramSize = 4000;
    public int datagramSize() {
        return datagramSize;
    }
    public FastCastConfiguration datagramSize(int datagramSize) {
        this.datagramSize = datagramSize;
        return this;
    }

    private int spinLoopMicros = 0;
    public int spinLoopMicros() {
        return spinLoopMicros;
    }
    public FastCastConfiguration spinLoopMicros(int spinLoopMicros) {
        this.spinLoopMicros = spinLoopMicros;
        return this;
    }

    private int threadParkMicros = 100;
    public int threadParkMicros() {
        return threadParkMicros;
    }
    public FastCastConfiguration threadParkMicros(int threadParkMicros) {
        this.threadParkMicros = threadParkMicros;
        return this;
    }

    private int packetsPerSecond = 30_000;
    public int packetsPerSecond() {
        return packetsPerSecond;
    }
    public FastCastConfiguration packetsPerSecond(int packetsPerSecond) {
        this.packetsPerSecond = packetsPerSecond;
        return this;
    }

    private int retransmissionPacketHistory = 100_000;
    public int retransmissionPacketHistory() {
        return retransmissionPacketHistory;
    }
    public void retransmissionPacketHistory(int retransmissionPacketHistory) {
        this.retransmissionPacketHistory = retransmissionPacketHistory;
    }

    private SocketConfiguration socketConfiguration = new SocketConfiguration();
    public SocketConfiguration socketConfiguration() {
        return socketConfiguration;
    }

    public static class SocketConfiguration {
        private int ttl = 8;
        public int ttl() {
            return ttl;
        }
        public void ttl(int ttl) {
            this.ttl = ttl;
        }

        private int socketReceiveBufferSize = 512_000;
        public int socketReceiveBufferSize() {
            return socketReceiveBufferSize;
        }
        public void socketReceiveBufferSize(int socketReceiveBufferSize) {
            this.socketReceiveBufferSize = socketReceiveBufferSize;
        }

        private int socketSendBufferSize = 128_000;
        public int socketSendBufferSize() {
            return socketSendBufferSize;
        }
        public void socketSendBufferSize(int socketSendBufferSize) {
            this.socketSendBufferSize = socketSendBufferSize;
        }
    }

}
