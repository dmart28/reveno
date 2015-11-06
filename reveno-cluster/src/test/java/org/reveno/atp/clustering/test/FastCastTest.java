package org.reveno.atp.clustering.test;

import org.junit.Assert;
import org.junit.Test;
import org.reveno.atp.clustering.api.Address;
import org.reveno.atp.clustering.api.ClusterView;
import org.reveno.atp.clustering.api.IOMode;
import org.reveno.atp.clustering.core.fastcast.FastCastBuffer;
import org.reveno.atp.clustering.core.fastcast.FastCastConfiguration;
import org.reveno.atp.clustering.util.Utils;
import org.reveno.atp.core.serialization.ProtostuffSerializer;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

public class FastCastTest {

    @Test
    public void test() throws Exception {
        int[] ports = Utils.getFreePorts(1);
        FastCastConfiguration config1 = basicConfig(ports[0]);
        config1.setCurrentNode(new Address("", IOMode.SYNC, "ab12"));
        config1.setNodeAddresses(Collections.singletonList(new Address("", IOMode.SYNC, "ab34")));

        FastCastBuffer buffer1 = new FastCastBuffer(config1);
        buffer1.connect();

        FastCastConfiguration config2 = basicConfig(ports[0]);
        config2.setCurrentNode(new Address("", IOMode.SYNC, "ab34"));
        config2.setNodeAddresses(Collections.singletonList(new Address("", IOMode.SYNC, "ab12")));

        FastCastBuffer buffer2 = new FastCastBuffer(config2);
        CountDownLatch latch = new CountDownLatch(1);
        ProtostuffSerializer serializer = new ProtostuffSerializer();
        buffer2.messageNotifier(serializer, l -> {
            Assert.assertEquals(1, l.size());
            Assert.assertEquals("test", ((TestEntity) l.get(0)).name);
            Assert.assertEquals(562, ((TestEntity) l.get(0)).num);
            latch.countDown();
        });
        buffer2.connect();

        ClusterView view = new ClusterView(1, Arrays.asList(config1.getCurrentNode(), config2.getCurrentNode()));
        buffer1.onView(view);
        buffer2.onView(view);

        serializer.registerTransactionType(TestEntity.class);
        serializer.serializeCommands(Collections.singletonList(new TestEntity("test", 562)), buffer1);

        Assert.assertTrue(buffer1.replicate());
    }

    protected FastCastConfiguration basicConfig(int port) {
        FastCastConfiguration config = new FastCastConfiguration();
        config.networkInterface("127.0.0.1");
        config.mcastPort(port);
        config.retransmissionPacketHistory(10_000);
        config.mcastHost("229.9.9.10");
        return config;
    }

    public static class TestEntity {
        public String name;
        public int num;

        public TestEntity(String name, int num) {
            this.name = name;
            this.num = num;
        }
    }

}
