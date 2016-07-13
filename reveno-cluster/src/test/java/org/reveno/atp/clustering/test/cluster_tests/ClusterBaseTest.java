package org.reveno.atp.clustering.test.cluster_tests;

import com.google.common.base.Strings;
import org.junit.Assert;
import org.reveno.atp.acceptance.api.commands.CreateNewAccountCommand;
import org.reveno.atp.acceptance.tests.RevenoBaseTest;
import org.reveno.atp.acceptance.views.AccountView;
import org.reveno.atp.acceptance.views.OrderView;
import org.reveno.atp.clustering.core.buffer.ClusterProvider;
import org.reveno.atp.clustering.test.common.ClusterEngineWrapper;
import org.reveno.atp.clustering.test.common.ClusterTestUtils;
import org.reveno.atp.clustering.util.Utils;
import org.reveno.atp.core.Engine;
import org.reveno.atp.test.utils.FileUtils;
import org.reveno.atp.utils.RevenoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.reveno.atp.utils.MeasureUtils.sec;

public class ClusterBaseTest extends RevenoBaseTest {

    protected void oneNodeFailInMiddleTest(Supplier<List<ClusterEngineWrapper>> enginesFactory) throws Exception {
        List<ClusterEngineWrapper> engines = enginesFactory.get();
        final ClusterEngineWrapper engine1 = engines.get(0);
        final ClusterEngineWrapper engine2 = engines.get(1);
        final ClusterEngineWrapper engine3 = engines.get(2);

        final ExecutorService executor = Executors.newFixedThreadPool(3);
        executor.execute(engine1::startup);
        executor.execute(engine2::startup);
        executor.execute(engine3::startup);

        waitInCluster(engine1, engine2, engine3);

        Assert.assertTrue(RevenoUtils.waitFor(() -> engine1.clusterStateInfo().isMaster(), sec(TIMEOUT_SECS)));
        long accountId = sendCommandSync(engine1, new CreateNewAccountCommand("USD", 1000_000L));

        Assert.assertTrue(RevenoUtils.waitFor(() -> engine2.query().findO(AccountView.class, accountId).isPresent(), sec(TIMEOUT_SECS)));
        Assert.assertTrue(RevenoUtils.waitFor(() -> engine3.query().findO(AccountView.class, accountId).isPresent(), sec(TIMEOUT_SECS)));

        long viewId = engine1.clusterStateInfo().currentView().viewId();
        long electionId = engine1.clusterStateInfo().electionId();
        int failed = generateAndSendCommands(engine1, 10_000, i -> {
            if (i == 6000) {
                engine2.shutdown();
                RevenoUtils.waitFor(() -> viewId != engine1.clusterStateInfo().currentView().viewId()
                        && electionId != engine1.clusterStateInfo().electionId(), sec(Integer.MAX_VALUE));
            }
        });

        // we fail and then reorganize again - nothing should be left
        Assert.assertEquals(0, failed);
        Assert.assertTrue(engine1.query().select(OrderView.class).size() == 10_000);

        engine1.shutdown();
        engine3.shutdown();
        executor.shutdown();
    }

    protected void waitInCluster(ClusterEngineWrapper... engines) {
        for (ClusterEngineWrapper engine : engines) {
            Assert.assertTrue(RevenoUtils.waitFor(() -> engine.isStarted() && engine.isElectedInCluster(), sec(TIMEOUT_SECS)));
        }
    }

    protected void complicatedSimultanousStartupTest(Supplier<List<ClusterEngineWrapper>> enginesFactory) throws Exception {
        List<ClusterEngineWrapper> engines = enginesFactory.get();
        final ClusterEngineWrapper engine1 = engines.get(0);
        final ClusterEngineWrapper engine2 = engines.get(1);
        final ExecutorService executor = Executors.newFixedThreadPool(2);

        executor.execute(engine1::startup);
        executor.execute(engine2::startup);

        waitInCluster(engine1, engine2);

        Assert.assertTrue(RevenoUtils.waitFor(() -> engine1.clusterStateInfo().isMaster(), sec(TIMEOUT_SECS)));

        generateAndSendCommands(engine1, 10_000);

        Assert.assertTrue(RevenoUtils.waitFor(() -> engine2.query().select(AccountView.class).size() == 10_000, sec(TIMEOUT_SECS)));
        Assert.assertTrue(RevenoUtils.waitFor(() -> engine2.query().select(OrderView.class).size() == 10_000, sec(TIMEOUT_SECS)));

        shutdownAll(engines);
        executor.shutdownNow();
    }

    protected void simultanousStartupTest(Supplier<List<ClusterEngineWrapper>> enginesFactory) throws Exception {
        List<ClusterEngineWrapper> engines = enginesFactory.get();
        final ClusterEngineWrapper engine1 = engines.get(0);
        final ClusterEngineWrapper engine2 = engines.get(1);
        final ExecutorService executor = Executors.newFixedThreadPool(2);

        executor.execute(engine1::startup);
        executor.execute(engine2::startup);

        Assert.assertTrue(RevenoUtils.waitFor(() -> engine1.clusterStateInfo().isMaster(), sec(TIMEOUT_SECS)));
        long accountId = sendCommandSync(engine1, new CreateNewAccountCommand("USD", 1000_000L));

        Assert.assertTrue(RevenoUtils.waitFor(() -> engine2.query().findO(AccountView.class, accountId).isPresent(), sec(TIMEOUT_SECS)));

        shutdownAll(engines);
        executor.shutdownNow();
    }

    protected void basicTest(Supplier<List<ClusterEngineWrapper>> enginesFactory, Consumer<ClusterEngineWrapper> forEach,
                             Supplier<ClusterProvider> provider) throws Exception {
        List<ClusterEngineWrapper> engines = enginesFactory.get();
        final ClusterEngineWrapper engine1 = engines.get(0);
        final ClusterEngineWrapper engine2 = engines.get(1);

        engine1.startup();
        engine2.startup();

        waitInCluster(engine1, engine2);

        Assert.assertTrue(RevenoUtils.waitFor(() -> engine1.clusterStateInfo().isMaster(), sec(TIMEOUT_SECS)));
        long accountId = sendCommandSync(engine1, new CreateNewAccountCommand("USD", 1000_000L));

        Assert.assertTrue(RevenoUtils.waitFor(() -> engine2.query().findO(AccountView.class, accountId).isPresent(), sec(TIMEOUT_SECS)));
        generateAndSendCommands(engine1, 10_000);

        LOG.info("1-2 Shutting down ...");
        engine1.shutdown();
        try {
            Assert.assertTrue(RevenoUtils.waitFor(() -> engine2.query().select(OrderView.class).size() == 10_000, sec(TIMEOUT_SECS)));
        } catch (Throwable t) {
            LOG.info("{}", engine2.query().select(OrderView.class).size());
            throw t;
        }
        engine2.shutdown();

        Engine replayEngine = configure(new Engine(engine2.getBaseDir()));
        replayEngine.startup();

        Assert.assertTrue(replayEngine.query().findO(AccountView.class, accountId).isPresent());
        Assert.assertEquals(10_001, replayEngine.query().select(AccountView.class).size());
        Assert.assertEquals(10_000, replayEngine.query().select(OrderView.class).size());

        replayEngine.shutdown();

        FileUtils.clearFolder(engine2.getBaseDir());

        LOG.info(Strings.repeat("-", 500));
        LOG.info("Starting cluster again, but with empty folder of node 2");
        engines = ClusterTestUtils.createClusterEngines(2, Arrays.asList(engine1.getBaseDir(), engine2.getBaseDir()), forEach, provider);
        final ClusterEngineWrapper engine3 = engines.get(0);
        final ClusterEngineWrapper engine4 = engines.get(1);

        engine3.startup();
        engine4.startup();

        waitInCluster(engine3, engine4);

        Assert.assertEquals(10_001, engine4.query().select(AccountView.class).size());
        Assert.assertEquals(10_000, engine4.query().select(OrderView.class).size());

        LOG.info("3-4 Shutting down ...");
        engine3.shutdown();
        engine4.shutdown();

        clearResources(engines);
    }

    protected void shutdownAll(List<ClusterEngineWrapper> engines) {
        engines.forEach(ClusterEngineWrapper::shutdown);
    }

    protected void clearResources(List<ClusterEngineWrapper> engines) {
        engines.forEach(e -> {
            try {
                FileUtils.delete(e.getBaseDir());
            } catch (IOException ignored) {
            }
        });
    }

    protected static final Logger LOG = LoggerFactory.getLogger(ClusterBaseTest.class);
    protected static final int TIMEOUT_SECS = 40;
}
