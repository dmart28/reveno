package org.reveno.atp.clustering.test;

import static org.reveno.atp.utils.MeasureUtils.sec;

import org.junit.Assert;
import org.junit.Test;
import org.reveno.atp.acceptance.api.commands.CreateNewAccountCommand;
import org.reveno.atp.acceptance.tests.RevenoBaseTest;
import org.reveno.atp.acceptance.views.AccountView;
import org.reveno.atp.acceptance.views.OrderView;
import org.reveno.atp.clustering.core.jgroups.JGroupsProvider;
import org.reveno.atp.clustering.test.common.ClusterEngineWrapper;
import org.reveno.atp.clustering.test.common.ClusterTestUtils;
import org.reveno.atp.clustering.util.Utils;
import org.reveno.atp.core.Engine;
import org.reveno.atp.test.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class ClusterTests extends RevenoBaseTest {

    @Test
    public void basicTestJGroups() throws Exception {
        setModelType();
        basicTest(ClusterTestUtils.createClusterEngines(2, this::configure, new JGroupsProvider("classpath:/tcp.xml")));
    }

    protected void basicTest(List<ClusterEngineWrapper> engines) throws Exception {
        ClusterEngineWrapper engine1 = engines.get(0);
        ClusterEngineWrapper engine2 = engines.get(1);

        engine1.startup();
        engine2.startup();

        Assert.assertTrue(Utils.waitFor(() -> engine1.failoverManager().isMaster(), sec(30)));
        long accountId = sendCommandSync(engine1, new CreateNewAccountCommand("USD", 1000_000L));

        Assert.assertTrue(Utils.waitFor(() -> engine2.query().find(AccountView.class, accountId).isPresent(), sec(30)));
        generateAndSendCommands(engine1, 10_000);

        LOG.info("Shutting down ...");
        engine1.shutdown();
        engine2.shutdown();

        Engine replayEngine = configure(new Engine(engine2.getBaseDir()));
        replayEngine.startup();

        Assert.assertTrue(replayEngine.query().find(AccountView.class, accountId).isPresent());
        Assert.assertEquals(10_001, replayEngine.query().select(AccountView.class).size());
        Assert.assertEquals(10_000, replayEngine.query().select(OrderView.class).size());

        replayEngine.shutdown();

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
