package org.reveno.atp.clustering.test;

import com.google.common.io.Files;
import org.junit.Test;
import org.reveno.atp.acceptance.tests.RevenoBaseTest;
import org.reveno.atp.clustering.api.Address;
import org.reveno.atp.clustering.api.IOMode;
import org.reveno.atp.clustering.api.InetAddress;
import org.reveno.atp.clustering.core.ClusterEngine;
import org.reveno.atp.clustering.core.jgroups.JGroupsProvider;
import org.reveno.atp.clustering.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.function.Supplier;

public class ClusterTests extends RevenoBaseTest {

    @Test
    public void basicTestJGroups() throws Exception {
        basicTest(() -> {
            File tempDir = Files.createTempDir();
            LOG.info(tempDir.getAbsolutePath());
            return new ClusterEngine(tempDir, getClass().getClassLoader(), new JGroupsProvider("classpath:/tcp.xml"));
        });
    }

    protected void basicTest(Supplier<ClusterEngine> engine) throws Exception {
        ClusterEngine engine1 = engine.get();
        ClusterEngine engine2 = engine.get();

        int[] ports = Utils.getFreePorts(2);
        Address address1 = new InetAddress("127.0.0.1:" + ports[0], IOMode.SYNC);
        Address address2 = new InetAddress("127.0.0.1:" + ports[1], IOMode.SYNC);
        engine1.clusterConfiguration().currentNodeAddress(address1);
        engine1.clusterConfiguration().clusterNodeAddresses(Collections.singletonList(address2));
        engine2.clusterConfiguration().currentNodeAddress(address2);
        engine2.clusterConfiguration().clusterNodeAddresses(Collections.singletonList(address1));
        engine1.clusterConfiguration().priority(1);
        engine2.clusterConfiguration().priority(2);

        engine1.startup();
        engine2.startup();

        System.out.println("o_o");

        engine1.shutdown();
        engine2.shutdown();
    }

    protected static final Logger LOG = LoggerFactory.getLogger(ClusterTests.class);

}
