package org.reveno.atp.clustering.core.fastcast;

import org.nustaq.fastcast.api.FCPublisher;
import org.nustaq.fastcast.api.FCSubscriber;
import org.nustaq.fastcast.api.FastCast;
import org.nustaq.offheap.bytez.Bytez;
import org.reveno.atp.clustering.api.ClusterBuffer;
import org.reveno.atp.clustering.core.components.AbstractClusterBuffer;
import org.reveno.atp.clustering.util.ResourceLoader;
import org.reveno.atp.clustering.util.Utils;
import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.api.serialization.TransactionInfoSerializer;
import org.reveno.atp.core.channel.NettyBasedBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

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
                return true;
            }

            @Override
            public void senderTerminated(String senderNodeId) {
            }

            @Override
            public void senderBootstrapped(String receivesFrom, long seqNo) {
            }
        });
        publisher = fastCast.onTransport(config.transportName()).publish(fastCast.getPublisherConf(config.topicName()));
    }

    @Override
    public void disconnect() {
        fastCast.getTransport(config.transportName()).close();
    }

    @Override
    public void messageNotifier(TransactionInfoSerializer serializer, Consumer<List<Object>> listener) {
        this.serializer = serializer;
        this.listener = listener;
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
            byteSource.setBuffer(sendBuffer);
            return publisher().offer(null, byteSource, 0, sendBuffer.limit(), true);
        } finally {
            sendBuffer.clear();
        }
    }

    protected FCPublisher publisher() {
        return publisher;
    }

    public FastCastBuffer(FastCastConfiguration config) throws Exception {
        fastCast = new FastCast();
        fastCast.setNodeId(config.nodeId());
        if (config.configFile().isPresent()) {
            fastCast.loadConfig(config.configFile().get().getAbsolutePath());
        } else {
            Properties props = new Properties();
            if (!Utils.isNullOrEmpty(config.mcastHost())) {
                props.put("fastcast.mcast.addr", config.mcastHost());
            }
            if (config.mcastPort() != 0) {
                props.put("fastcast.mcast.port", String.valueOf(config.mcastPort()));
            }
            if (!Utils.isNullOrEmpty(config.networkInterface())) {
                props.put("fastcast.interface", config.networkInterface());
            }
            String configStr = ResourceLoader.loadResource(
                    getClass().getClassLoader().getResourceAsStream("fastcast_default.kson"), props);
            File configFile = Files.createTempFile("cfg", ".kson").toFile();
            Utils.write(configStr, configFile);
            fastCast.loadConfig(configFile.getAbsolutePath());
            configFile.delete();
        }
        this.config = config;
    }

    protected FastCast fastCast;
    protected FastCastConfiguration config;
    protected TransactionInfoSerializer serializer;
    protected Consumer<List<Object>> listener;

    protected FCPublisher publisher;

    protected BytezBufferWrapper bytezBuffer = new BytezBufferWrapper();
    protected ByteSourceBuffer byteSource = new ByteSourceBuffer();

    protected static final Logger LOG = LoggerFactory.getLogger(FastCastBuffer.class);

    protected volatile boolean locked = false;
}
