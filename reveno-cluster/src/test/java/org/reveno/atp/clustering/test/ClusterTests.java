package org.reveno.atp.clustering.test;

import static org.reveno.atp.utils.MeasureUtils.sec;

import com.google.common.base.Strings;
import org.junit.Assert;
import org.junit.Test;
import org.reveno.atp.acceptance.api.commands.CreateNewAccountCommand;
import org.reveno.atp.acceptance.tests.RevenoBaseTest;
import org.reveno.atp.acceptance.views.AccountView;
import org.reveno.atp.acceptance.views.OrderView;
import org.reveno.atp.clustering.api.SyncMode;
import org.reveno.atp.clustering.core.buffer.ClusterProvider;
import org.reveno.atp.clustering.core.jgroups.JGroupsProvider;
import org.reveno.atp.clustering.test.common.ClusterEngineWrapper;
import org.reveno.atp.clustering.test.common.ClusterTestUtils;
import org.reveno.atp.clustering.util.Utils;
import org.reveno.atp.core.Engine;
import org.reveno.atp.test.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class ClusterTests extends RevenoBaseTest {

    @Test
    public void basicTestJGroups() throws Exception {
        setModelType();
        Supplier<ClusterProvider> provider = () -> new JGroupsProvider("classpath:/tcp.xml");
        basicTest(() -> ClusterTestUtils.createClusterEngines(2, this::configure, provider), provider);
    }

    @Test
    public void journalsReplicationTestJGroups() throws Exception {
        setModelType();
        Supplier<ClusterProvider> provider = () -> new JGroupsProvider("classpath:/tcp.xml");
        basicTest(() -> ClusterTestUtils.createClusterEngines(2, e -> {
            this.configure(e);
            e.clusterConfiguration().sync().mode(SyncMode.JOURNALS);
        }, provider), provider);
    }

    protected void basicTest(Supplier<List<ClusterEngineWrapper>> enginesFactory, Supplier<ClusterProvider> provider) throws Exception {
        List<ClusterEngineWrapper> engines = enginesFactory.get();
        final ClusterEngineWrapper engine1 = engines.get(0);
        final ClusterEngineWrapper engine2 = engines.get(1);

        engine1.startup();
        engine2.startup();

        Assert.assertTrue(Utils.waitFor(() -> engine1.failoverManager().isMaster(), sec(30)));
        long accountId = sendCommandSync(engine1, new CreateNewAccountCommand("USD", 1000_000L));

        Assert.assertTrue(Utils.waitFor(() -> engine2.query().find(AccountView.class, accountId).isPresent(), sec(30)));
        generateAndSendCommands(engine1, 10_000);

        LOG.info("1-2 Shutting down ...");
        engine1.shutdown();
        engine2.shutdown();

        Engine replayEngine = configure(new Engine(engine2.getBaseDir()));
        replayEngine.startup();

        Assert.assertTrue(replayEngine.query().find(AccountView.class, accountId).isPresent());
        Assert.assertEquals(10_001, replayEngine.query().select(AccountView.class).size());
        Assert.assertEquals(10_000, replayEngine.query().select(OrderView.class).size());

        replayEngine.shutdown();

        FileUtils.clearFolder(engine2.getBaseDir());

        LOG.info(Strings.repeat("-", 50));
        LOG.info("Starting cluster again, but with empty folder of node 2");
        engines = ClusterTestUtils.createClusterEngines(2, Arrays.asList(engine1.getBaseDir(), engine2.getBaseDir()), this::configure, provider);
        final ClusterEngineWrapper engine3 = engines.get(0);
        final ClusterEngineWrapper engine4 = engines.get(1);

        engine3.startup();
        engine4.startup();

        Assert.assertEquals(10_001, engine4.query().select(AccountView.class).size());
        Assert.assertEquals(10_000, engine4.query().select(OrderView.class).size());

        LOG.info("3-4 Shutting down ...");
        engine3.shutdown();
        engine4.shutdown();

        clearResources(engines);
    }

    protected void clearResources(List<ClusterEngineWrapper> engines) {
        engines.forEach(e -> {
            try {
                FileUtils.delete(e.getBaseDir());
            } catch (IOException ignored) {
            }
        });
    }

    protected static final Logger LOG = LoggerFactory.getLogger(ClusterTests.class);

}
