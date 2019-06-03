package org.reveno.atp.core.commons;

import org.junit.Assert;
import org.junit.Test;
import org.reveno.atp.commons.ByteArrayObjectMap;

import java.util.Arrays;
import java.util.Random;

public class ByteArrayObjectMapTest {

    @Test
    public void test() throws Exception {
        ByteArrayObjectMap<String> map = new ByteArrayObjectMap<>();
        byte[] key1 = new byte[20];
        new Random().nextBytes(key1);

        map.put(key1, "Hello world!");
        Assert.assertEquals("Hello world!", map.get(key1));
        Assert.assertNull(map.get(Arrays.copyOfRange(key1, 0, 6)));
    }

}
