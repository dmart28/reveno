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
import org.reveno.atp.clustering.core.providers.MulticastAllProvider;
import org.reveno.atp.clustering.core.providers.UnicastAllProvider;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ClusterTests extends RevenoBaseTest {

    @Test
    public void basicTestMulticast() throws Exception {
        setModelType();
        Consumer<ClusterEngineWrapper> forEach = e -> {
            this.configure(e);
            e.clusterConfiguration().multicast().host("229.9.9.10");
            e.clusterConfiguration().multicast().port(47365);
            e.clusterConfiguration().multicast().preferBatchingToLatency(true);
        };
        basicTest(() -> ClusterTestUtils.createClusterEngines(2, forEach, MulticastAllProvider::new), forEach,
                MulticastAllProvider::new);
    }

    @Test
    public void basicTestJGroups() throws Exception {
        setModelType();
        basicTest(() -> ClusterTestUtils.createClusterEngines(2, this::configure, UnicastAllProvider::new),
                this::configure, UnicastAllProvider::new);
    }

    @Test
    public void journalsReplicationTestJGroups() throws Exception {
        setModelType();
        Consumer<ClusterEngineWrapper> forEach = e -> {
            this.configure(e);
            e.clusterConfiguration().dataSync().mode(SyncMode.JOURNALS);
        };
        basicTest(() -> ClusterTestUtils.createClusterEngines(2, forEach, UnicastAllProvider::new),
                forEach, UnicastAllProvider::new);
    }

    @Test
    public void simultanousStartupTestJGroups() throws Exception {
        setModelType();
        simultanousStartupTest(() -> ClusterTestUtils.createClusterEngines(2, this::configure, UnicastAllProvider::new));
    }

    @Test
    public void simultanousStartupTestFastCast() throws Exception {
        setModelType();
        Consumer<ClusterEngineWrapper> forEach = e -> {
            this.configure(e);
            e.clusterConfiguration().multicast().host("229.9.9.10");
            e.clusterConfiguration().multicast().port(47366);
            e.clusterConfiguration().multicast().sendRetries(100_000);
        };
        simultanousStartupTest(() -> ClusterTestUtils.createClusterEngines(2, forEach, MulticastAllProvider::new));
    }

    @Test
    public void complicatedSimultanousStartupTestJGroups() throws Exception {
        setModelType();
        complicatedSimultanousStartupTest(() -> ClusterTestUtils.createClusterEngines(2, this::configure, UnicastAllProvider::new));
    }

    @Test
    public void complicatedSimultanousStartupTestFastCast() throws Exception {
        setModelType();
        Consumer<ClusterEngineWrapper> forEach = e -> {
            this.configure(e);
            e.clusterConfiguration().multicast().host("229.9.9.10");
            e.clusterConfiguration().multicast().port(47367);
            e.clusterConfiguration().multicast().sendRetries(100_000);
        };
        complicatedSimultanousStartupTest(() -> ClusterTestUtils.createClusterEngines(2, forEach, MulticastAllProvider::new));
    }

    @Test
    public void oneNodeFailInMiddleTestJGroups() throws Exception {
        setModelType();
        oneNodeFailInMiddleTest(() -> ClusterTestUtils.createClusterEngines(3, this::configure, UnicastAllProvider::new));
    }

    @Test
    public void oneNodeFailInMiddleTestFastCast() throws Exception {
        setModelType();
        Consumer<ClusterEngineWrapper> forEach = e -> {
            this.configure(e);
            e.clusterConfiguration().multicast().host("229.9.9.10");
            e.clusterConfiguration().multicast().port(47368);
            e.clusterConfiguration().multicast().sendRetries(100_000);
        };
        oneNodeFailInMiddleTest(() -> ClusterTestUtils.createClusterEngines(3, forEach, MulticastAllProvider::new));
    }

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

        Assert.assertTrue(Utils.waitFor(() -> engine1.failoverManager() != null &&
                engine1.failoverManager().isMaster(), sec(30)));
        long accountId = sendCommandSync(engine1, new CreateNewAccountCommand("USD", 1000_000L));

        Assert.assertTrue(Utils.waitFor(() -> engine2.query().find(AccountView.class, accountId).isPresent(), sec(30)));
        Assert.assertTrue(Utils.waitFor(() -> engine3.query().find(AccountView.class, accountId).isPresent(), sec(30)));

        int failed = generateAndSendCommands(engine1, 10_000, i -> {
            if (i == 6000) {
                engine2.shutdown();
            }
        });

        Assert.assertEquals(10_000 - failed, engine1.query().select(OrderView.class).size());

        engine1.shutdown();
        engine3.shutdown();
        executor.shutdown();
    }

    protected void waitInCluster(ClusterEngineWrapper... engines) {
        for (ClusterEngineWrapper engine : engines) {
            Assert.assertTrue(Utils.waitFor(() -> engine.failoverManager() != null && engine.isInCluster(), sec(30)));
        }
    }

    protected void complicatedSimultanousStartupTest(Supplier<List<ClusterEngineWrapper>> enginesFactory) throws Exception {
        List<ClusterEngineWrapper> engines = enginesFactory.get();
        final ClusterEngineWrapper engine1 = engines.get(0);
        final ClusterEngineWrapper engine2 = engines.get(1);
        final ExecutorService executor = Executors.newFixedThreadPool(2);

        executor.execute(engine1::startup);
        executor.execute(engine2::startup);

        Assert.assertTrue(Utils.waitFor(() -> engine1.failoverManager() != null &&
                engine1.failoverManager().isMaster(), sec(30)));

        generateAndSendCommands(engine1, 10_000);

        Assert.assertTrue(Utils.waitFor(() -> engine2.query().select(AccountView.class).size() == 10_000, sec(30)));
        Assert.assertTrue(Utils.waitFor(() -> engine2.query().select(OrderView.class).size() == 10_000, sec(30)));

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

        Assert.assertTrue(Utils.waitFor(() -> engine1.failoverManager() != null &&
                engine1.failoverManager().isMaster(), sec(30)));
        long accountId = sendCommandSync(engine1, new CreateNewAccountCommand("USD", 1000_000L));

        Assert.assertTrue(Utils.waitFor(() -> engine2.query().find(AccountView.class, accountId).isPresent(), sec(30)));

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

        Assert.assertTrue(Utils.waitFor(() -> engine1.failoverManager().isMaster(), sec(30)));
        long accountId = sendCommandSync(engine1, new CreateNewAccountCommand("USD", 1000_000L));

        Assert.assertTrue(Utils.waitFor(() -> engine2.query().find(AccountView.class, accountId).isPresent(), sec(30)));
        generateAndSendCommands(engine1, 10_000);

        LOG.info("1-2 Shutting down ...");
        engine1.shutdown();
        try {
            Assert.assertTrue(Utils.waitFor(() -> engine2.query().select(OrderView.class).size() == 10_000, sec(30)));
        } catch (Throwable t) {
            LOG.info("{}", engine2.query().select(OrderView.class).size());
            throw t;
        }
        engine2.shutdown();

        Engine replayEngine = configure(new Engine(engine2.getBaseDir()));
        replayEngine.startup();

        Assert.assertTrue(replayEngine.query().find(AccountView.class, accountId).isPresent());
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

    protected static final Logger LOG = LoggerFactory.getLogger(ClusterTests.class);

}
