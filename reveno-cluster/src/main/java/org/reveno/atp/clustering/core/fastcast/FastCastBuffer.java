package org.reveno.atp.clustering.core.fastcast;

import org.nustaq.fastcast.api.FCPublisher;
import org.nustaq.fastcast.api.FCSubscriber;
import org.nustaq.fastcast.api.FastCast;
import org.nustaq.fastcast.config.*;
import org.nustaq.fastcast.impl.PacketSendBuffer;
import org.nustaq.offheap.bytez.Bytez;
import org.reveno.atp.clustering.api.*;
import org.reveno.atp.clustering.core.components.AbstractClusterBuffer;
import org.reveno.atp.clustering.util.ResourceLoader;
import org.reveno.atp.clustering.util.Utils;
import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.api.serialization.TransactionInfoSerializer;
import org.reveno.atp.core.channel.NettyBasedBuffer;
import org.reveno.atp.utils.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * fast-cast {@link ClusterBuffer} implementation, which is based on reliable
 * message multicasting. Still, it is impossible to build {@link org.reveno.atp.clustering.api.Cluster}
 * on top of fast-cast, so {@link org.reveno.atp.clustering.core.jgroups.JGroupsCluster} or another
 * implementation must be used.
 */
public class FastCastBuffer extends AbstractClusterBuffer implements ClusterBuffer {

    @Override
    public void connect() {
        fastCast.onTransport(config.transportName()).subscribe(fastCast.getSubscriberConf(config.topicName()), new FCSubscriber() {
            @Override
            public void messageReceived(String sender, long sequence, Bytez b, long off, int len) {
                try {
                    if (!locked) {
                        bytezBuffer.setBytez(b, off, len);
                        listener.accept(serializer.deserializeCommands(bytezBuffer));
                    }
                } catch (Throwable t) {
                    LOG.error("messageReceived", t);
                }
            }

            @Override
            public boolean dropped() {
                LOG.info("FCST {}: dropped, failover mode enabled", config.getCurrentNode().getNodeId());
                locked = true;
                // possible fatal case for us - init leadership election & sync
                // this will notify ALL NODES to start leadership election process
                failoverNotifier.accept(ClusterEvent.MEMBERSHIP_CHANGED);
                return true;
            }

            @Override
            public void senderTerminated(String senderNodeId) {
                LOG.info("FCST {}: member [{}] leaves.", config.getCurrentNode().getNodeId(), senderNodeId);
                synchronized (FastCastBuffer.this) {
                    senders.remove(senderNodeId);
                    recalculateEligability(false);
                }
            }

            @Override
            public void senderBootstrapped(String receivesFrom, long seqNo) {
                LOG.info("FCST {}: new member [{}] joins.", config.getCurrentNode().getNodeId(), receivesFrom);
                synchronized (FastCastBuffer.this) {
                    senders.add(receivesFrom);
                    recalculateEligability(false);
                }
            }
        });
        publisher = fastCast.onTransport(config.transportName()).publish(fastCast.getPublisherConf(config.topicName()));
    }

    @Override
    public void disconnect() {
        publisher.flush();
        // to make sure it's really flushed ...
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {
        }
        fastCast.getTransportDriver(config.transportName()).terminate();
        fastCast.getTransport(config.transportName()).close();
        if (publisher instanceof PacketSendBuffer) {
            ((PacketSendBuffer)publisher).free();
        }
    }

    @Override
    public void onView(ClusterView view) {
        this.view = view;
        recalculateEligability(true);
    }

    @Override
    public void messageNotifier(TransactionInfoSerializer serializer, Consumer<List<Object>> listener) {
        this.serializer = serializer;
        this.listener = listener;
    }

    @Override
    public void failoverNotifier(Consumer<ClusterEvent> listener) {
        this.failoverNotifier = listener;
    }

    @Override
    public void lockIncoming() {
        locked = true;
    }

    @Override
    public void unlockIncoming() {
        locked = false;
    }

    @Override
    public void erase() {
    }

    @Override
    public void prepare() {
    }

    @Override
    public boolean replicate() {
        try {
            if (!isEligableToSend) {
                return false;
            }

            byteSource.setBuffer(sendBuffer);
            boolean res = false;
            int count = 0;
            while (!res && count < config.sendRetries()) {
                res = publisher().offer(null, byteSource, 0, sendBuffer.limit(), config.alwaysFlush());
                count++;
            }
            if (LOG.isDebugEnabled() && !res) {
                LOG.warn("FCST: Can't send to FC!");
            }
            return res;
        } finally {
            sendBuffer.clear();
        }
    }

    protected FCPublisher publisher() {
        return publisher;
    }

    protected synchronized void recalculateEligability(boolean newView) {
        if (view == null) {
            isEligableToSend = false;
            return;
        }
        List<String> viewNodes = view.members().stream().map(Address::getNodeId).collect(Collectors.toList());
        List<Address> viewAddresses = config.getNodeAddresses().stream()
                .filter(addressInConfig -> viewNodes.contains(addressInConfig.getNodeId()))
                .collect(Collectors.toList());
        if (newView) {
            senders.clear();
            senders.addAll(viewAddresses.stream().map(Address::getNodeId).collect(Collectors.toList()));
        }
        isEligableToSend = viewAddresses.size() == senders.size() &&
                viewAddresses.stream().map(Address::getNodeId).allMatch(senders::contains);
    }

    public FastCastBuffer(FastCastConfiguration config) {
        try {
            fastCast = new FastCastEx();
            fastCast.setNodeId(config.getCurrentNode().getNodeId());

            PhysicalTransportConf transportConf = new PhysicalTransportConf();
            transportConf.setDgramsize(config.datagramSize());
            transportConf.ttl(config.socketConfiguration().ttl());
            transportConf.socketReceiveBufferSize(config.socketConfiguration().socketReceiveBufferSize());
            transportConf.socketSendBufferSize(config.socketConfiguration().socketSendBufferSize());
            transportConf.port(config.mcastPort());
            transportConf.mulitcastAdr(config.mcastHost());
            transportConf.interfaceAdr(config.networkInterface());
            transportConf.idleParkMicros(config.threadParkMicros());
            transportConf.setName(config.transportName());
            transportConf.spinLoopMicros(config.spinLoopMicros());

            PublisherConf publisherConf = new PublisherConf(1);
            publisherConf.heartbeatInterval(20);
            publisherConf.numPacketHistory(config.retransmissionPacketHistory());
            publisherConf.pps(config.packetsPerSecond());
            publisherConf.ppsWindow(10);

            SubscriberConf subscriberConf = new SubscriberConf(1);
            subscriberConf.receiveBufferPackets(10_000);

            TopicConf topicConf = new TopicConf().id(1);
            topicConf.name(config.topicName());
            topicConf.publisher(publisherConf);
            topicConf.subscriber(subscriberConf);

            ClusterConf clusterConf = new ClusterConf();
            clusterConf.transports(transportConf).topics(topicConf);

            fastCast.setConfig(clusterConf);
            //fastCast.addTransportsFrom(clusterConf);

            this.config = config;
        } catch (Throwable t) {
            throw Exceptions.runtime(t);
        }
    }

    protected volatile ClusterView view;
    protected volatile boolean isEligableToSend = false;

    protected FastCast fastCast;
    protected FastCastConfiguration config;
    protected TransactionInfoSerializer serializer;
    protected Consumer<List<Object>> listener;
    protected Consumer<ClusterEvent> failoverNotifier;

    protected FCPublisher publisher;
    protected Set<String> senders = Collections.newSetFromMap(new ConcurrentHashMap<>());

    protected BytezBufferWrapper bytezBuffer = new BytezBufferWrapper();
    protected ByteSourceBuffer byteSource = new ByteSourceBuffer();

    protected static final Logger LOG = LoggerFactory.getLogger(FastCastBuffer.class);

    protected volatile boolean locked = false;
}
