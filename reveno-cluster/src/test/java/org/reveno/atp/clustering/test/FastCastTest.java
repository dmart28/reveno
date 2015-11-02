package org.reveno.atp.clustering.test;

import org.junit.Assert;
import org.junit.Test;
import org.reveno.atp.clustering.core.fastcast.FastCastBuffer;
import org.reveno.atp.clustering.core.fastcast.FastCastConfiguration;
import org.reveno.atp.clustering.util.Utils;
import org.reveno.atp.core.serialization.ProtostuffSerializer;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;

public class FastCastTest {

    @Test
    public void test() throws Exception {
        int[] ports = Utils.getFreePorts(1);
        FastCastConfiguration config = new FastCastConfiguration();
        config.networkInterface("en0");
        config.mcastPort(ports[0]);

        FastCastBuffer buffer1 = new FastCastBuffer(config);
        buffer1.connect();

        FastCastBuffer buffer2 = new FastCastBuffer(config);
        CountDownLatch latch = new CountDownLatch(1);
        ProtostuffSerializer serializer = new ProtostuffSerializer();
        buffer2.messageNotifier(serializer, l -> {
            Assert.assertEquals(1, l.size());
            Assert.assertEquals("test", ((TestEntity)l.get(0)).name);
            Assert.assertEquals(562, ((TestEntity)l.get(0)).num);
            latch.countDown();
        });
        buffer2.connect();

        serializer.registerTransactionType(TestEntity.class);
        serializer.serializeCommands(Collections.singletonList(new TestEntity("test", 562)), buffer1);

        //Thread.sleep(5000);
        Assert.assertTrue(buffer1.replicate());
        latch.await();
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
