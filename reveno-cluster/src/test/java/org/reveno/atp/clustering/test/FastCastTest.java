package org.reveno.atp.clustering.test;

import org.junit.Test;
import org.reveno.atp.clustering.core.fastcast.FastCastBuffer;
import org.reveno.atp.clustering.core.fastcast.FastCastConfiguration;

public class FastCastTest {

    @Test
    public void test() throws Exception {
        FastCastConfiguration config = new FastCastConfiguration();
        FastCastBuffer buffer = new FastCastBuffer(config);
    }

}
