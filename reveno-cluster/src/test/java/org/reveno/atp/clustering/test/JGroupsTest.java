package org.reveno.atp.clustering.test;

import org.junit.Assert;
import org.junit.Test;
import org.reveno.atp.clustering.api.Cluster;
import org.reveno.atp.clustering.api.ClusterConnector;
import org.reveno.atp.clustering.api.IOMode;
import org.reveno.atp.clustering.api.InetAddress;
import org.reveno.atp.clustering.api.message.Message;
import org.reveno.atp.clustering.core.RevenoClusterConfiguration;
import org.reveno.atp.clustering.core.jgroups.JGroupsProvider;
import org.reveno.atp.clustering.util.Utils;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class JGroupsTest {

    @Test
    public void clusterBasicSetupTest() throws Exception {
        int[] ports = Utils.getFreePorts(3);

        JGroupsProvider provider1 = jgroupsProvider(ports[0], new int[]{ports[1], ports[2]});
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
        cluster1.gateway().receive(TestMessage.TYPE, m -> count.incrementAndGet());
        cluster2.gateway().receive(TestMessage.TYPE, m -> count.incrementAndGet());
        cluster3.gateway().receive(TestMessage.TYPE, m -> count.incrementAndGet());

        cluster1.gateway().send(cluster1.view().members(), new TestMessage("Hello!"));

        Assert.assertTrue(Utils.waitFor(() -> count.get() == 2, 1000));
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
        public int type() {
            return TYPE;
        }

        private String data;

        public TestMessage(String data) {
            this.data = data;
        }

        public TestMessage() {
        }
    }

}
