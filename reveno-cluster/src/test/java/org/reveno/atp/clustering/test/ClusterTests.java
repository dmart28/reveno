package org.reveno.atp.clustering.test;

import static org.reveno.atp.utils.MeasureUtils.sec;

import com.google.common.io.Files;
import org.junit.Assert;
import org.junit.Test;
import org.reveno.atp.acceptance.api.commands.CreateNewAccountCommand;
import org.reveno.atp.acceptance.tests.RevenoBaseTest;
import org.reveno.atp.acceptance.views.AccountView;
import org.reveno.atp.clustering.api.Address;
import org.reveno.atp.clustering.api.IOMode;
import org.reveno.atp.clustering.api.InetAddress;
import org.reveno.atp.clustering.core.ClusterEngine;
import org.reveno.atp.clustering.core.jgroups.JGroupsProvider;
import org.reveno.atp.clustering.util.Tuple;
import org.reveno.atp.clustering.util.Utils;
import org.reveno.atp.core.Engine;
import org.reveno.atp.test.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.function.Supplier;

public class ClusterTests extends RevenoBaseTest {

    @Test
    public void basicTestJGroups() throws Exception {
        setModelType();
        basicTest(() -> {
            File tempDir = Files.createTempDir();
            LOG.info(tempDir.getAbsolutePath());
            return new Tuple<>(tempDir,
                    configure(new ClusterEngine(tempDir, getClass().getClassLoader(),
                            new JGroupsProvider("classpath:/tcp.xml"))));
        });
    }

    protected void basicTest(Supplier<Tuple<File, ClusterEngine>> tuple) throws Exception {
        Tuple<File, ClusterEngine> tuple1 = tuple.get();
        ClusterEngine engine1 = tuple1.getVal2();
        Tuple<File, ClusterEngine> tuple2 = tuple.get();
        ClusterEngine engine2 = tuple2.getVal2();

        int[] ports = Utils.getFreePorts(2);
        Address address1 = new InetAddress("127.0.0.1:" + ports[0], IOMode.SYNC);
        Address address2 = new InetAddress("127.0.0.1:" + ports[1], IOMode.SYNC);
        engine1.clusterConfiguration().currentNodeAddress(address1);
        engine1.clusterConfiguration().clusterNodeAddresses(Collections.singletonList(address2));
        engine2.clusterConfiguration().currentNodeAddress(address2);
        engine2.clusterConfiguration().clusterNodeAddresses(Collections.singletonList(address1));
        engine1.clusterConfiguration().priority(2);
        engine2.clusterConfiguration().priority(1);

        engine1.startup();
        engine2.startup();

        Assert.assertTrue(Utils.waitFor(() -> engine1.failoverManager().isMaster(), sec(30)));
        long accountId = sendCommandSync(engine1, new CreateNewAccountCommand("USD", 1000_000L));

        Assert.assertTrue(Utils.waitFor(() -> engine2.query().find(AccountView.class, accountId).isPresent(), sec(30)));

        LOG.info("Shutting down ...");
        engine1.shutdown();
        engine2.shutdown();

        Engine replayEngine = configure(new Engine(tuple2.getVal1()));
        replayEngine.startup();

        Assert.assertTrue(replayEngine.query().find(AccountView.class, accountId).isPresent());

        replayEngine.shutdown();

        FileUtils.delete(tuple1.getVal1());
        FileUtils.delete(tuple2.getVal1());
    }

    protected static final Logger LOG = LoggerFactory.getLogger(ClusterTests.class);

}
