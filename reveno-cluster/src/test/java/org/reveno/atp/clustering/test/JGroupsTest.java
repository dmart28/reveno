package org.reveno.atp.clustering.test;

import org.junit.Assert;
import org.junit.Test;
import org.reveno.atp.clustering.api.Cluster;
import org.reveno.atp.clustering.api.ClusterBuffer;
import org.reveno.atp.clustering.api.IOMode;
import org.reveno.atp.clustering.api.InetAddress;
import org.reveno.atp.clustering.api.message.Message;
import org.reveno.atp.clustering.core.RevenoClusterConfiguration;
import org.reveno.atp.clustering.core.jgroups.JGroupsProvider;
import org.reveno.atp.clustering.util.Utils;
import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.serialization.ProtostuffSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class JGroupsTest {

    @Test
    public void clusterBasicSetupTest() throws Exception {
        int[] ports = Utils.getFreePorts(3);

        JGroupsProvider provider1 = jgroupsProvider(ports[0], new int[] {ports[1], ports[2]});
        JGroupsProvider provider2 = jgroupsProvider(ports[1], new int[] {ports[0], ports[2]});
        JGroupsProvider provider3 = jgroupsProvider(ports[2], new int[] {ports[0], ports[1]});

        Cluster cluster1 = provider1.retrieveCluster();
        Cluster cluster2 = provider2.retrieveCluster();
        Cluster cluster3 = provider3.retrieveCluster();

        cluster1.connect(); cluster2.connect(); cluster3.connect();

        Assert.assertTrue(cluster1.isConnected());
        Assert.assertTrue(cluster2.isConnected());
        Assert.assertTrue(cluster3.isConnected());

        AtomicInteger count = new AtomicInteger();
        cluster1.gateway().receive(TestMessage.TYPE, m -> { LOG.info("MSG1:{}", m); count.incrementAndGet(); });
        cluster2.gateway().receive(TestMessage.TYPE, m -> { LOG.info("MSG2:{}", m); count.incrementAndGet(); });
        cluster3.gateway().receive(TestMessage.TYPE, m -> { LOG.info("MSG3:{}", m); count.incrementAndGet(); });

        cluster1.gateway().send(cluster1.view().members(), new TestMessage("Hello!"));

        Assert.assertTrue(Utils.waitFor(() -> count.get() == 2, TEST_TIMEOUT));
        count.set(0);

        cluster2.disconnect();

        cluster1.gateway().send(cluster1.view().members(), new TestMessage("World!"));

        Assert.assertTrue(Utils.waitFor(() -> count.get() == 1, TEST_TIMEOUT));

        cluster1.disconnect();
        cluster3.disconnect();
    }

    @Test
    public void jgroupsBufferTest() throws Exception {
        int[] ports = Utils.getFreePorts(3);

        JGroupsProvider provider1 = jgroupsProvider(ports[0], new int[]{ports[1], ports[2]});
        JGroupsProvider provider2 = jgroupsProvider(ports[1], new int[]{ports[0], ports[2]});
        JGroupsProvider provider3 = jgroupsProvider(ports[2], new int[]{ports[0], ports[1]});

        ClusterBuffer buffer1 = provider1.retrieveBuffer();
        ClusterBuffer buffer2 = provider2.retrieveBuffer();
        ClusterBuffer buffer3 = provider3.retrieveBuffer();

        buffer1.connect(); buffer2.connect(); buffer3.connect();

        TestMessage message = new TestMessage("Hello world!");
        ProtostuffSerializer serializer = new ProtostuffSerializer();
        serializer.registerTransactionType(TestMessage.class);

        AtomicInteger count = new AtomicInteger(2);
        final Consumer<ClusterBuffer> clusterBufferConsumer = b -> {
            serializer.deserializeObject(b);
            count.decrementAndGet();
        };
        buffer2.messageNotifier(clusterBufferConsumer);
        buffer3.messageNotifier(clusterBufferConsumer);

        serializer.serializeObject(buffer1, message);
        buffer1.replicate();

        Assert.assertTrue(Utils.waitFor(() -> count.get() == 0, TEST_TIMEOUT));

        count.set(1);
        buffer2.lockIncoming();
        serializer.serializeObject(buffer1, message);
        buffer1.replicate();

        Assert.assertTrue(Utils.waitFor(() -> count.get() == 0, TEST_TIMEOUT));
        Assert.assertFalse(Utils.waitFor(() -> count.get() < 0, TEST_SHORT_TIMEOUT));

        count.set(2);
        buffer2.unlockIncoming();
        serializer.serializeObject(buffer1, message);
        buffer1.replicate();

        Assert.assertTrue(Utils.waitFor(() -> count.get() == 0, TEST_TIMEOUT));
        Assert.assertFalse(Utils.waitFor(() -> count.get() < 0, TEST_SHORT_TIMEOUT));

        buffer1.disconnect(); buffer2.disconnect(); buffer3.disconnect();
    }

    protected static JGroupsProvider jgroupsProvider(int port, int[] nodes) {
        RevenoClusterConfiguration config = new RevenoClusterConfiguration();
        config.clusterNodeAddresses(IntStream.of(nodes).mapToObj(JGroupsTest::inet).collect(Collectors.toList()));
        config.currentNodeAddress(inet(port));

        JGroupsProvider provider = new JGroupsProvider("classpath:/tcp.xml");
        provider.initialize(config);
        return provider;
    }

    protected static InetAddress inet(int port) {
        return new InetAddress("127.0.0.1:" + port, IOMode.SYNC);
    }

    protected static class TestMessage extends Message {
        public static final int TYPE = 0x0123456;

        @Override
        public void write(Buffer buffer) {
        }

        @Override
        public void read(Buffer buffer) {
        }

        @Override
        public int type() {
            return TYPE;
        }

        private String data;

        public TestMessage(String data) {
            this.data = data;
        }

        public TestMessage() {
        }

        @Override
        public String toString() {
            return "TestMessage{" +
                    "data='" + data + '\'' +
                    "address='" + address() + '\'' +
                    '}';
        }
    }

    protected static final int TEST_TIMEOUT = 5000;
    protected static final int TEST_SHORT_TIMEOUT = TEST_TIMEOUT / 10;
    protected static final Logger LOG = LoggerFactory.getLogger(JGroupsTest.class);
}
