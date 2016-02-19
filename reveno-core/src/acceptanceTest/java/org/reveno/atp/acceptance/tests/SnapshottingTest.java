package org.reveno.atp.acceptance.tests;

import org.junit.Assert;
import org.junit.Test;
import org.reveno.atp.acceptance.api.events.AccountCreatedEvent;
import org.reveno.atp.acceptance.api.events.OrderCreatedEvent;
import org.reveno.atp.acceptance.views.AccountView;
import org.reveno.atp.acceptance.views.OrderView;
import org.reveno.atp.api.RepositorySnapshotter;
import org.reveno.atp.api.Reveno;
import org.reveno.atp.api.domain.RepositoryData;
import org.reveno.atp.core.RevenoConfiguration;
import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.api.serialization.RepositoryDataSerializer;
import org.reveno.atp.core.channel.NettyBasedBuffer;
import org.reveno.atp.core.serialization.DefaultJavaSerializer;
import org.reveno.atp.core.serialization.ProtostuffSerializer;
import org.reveno.atp.core.snapshots.DefaultSnapshotter;
import org.reveno.atp.core.storage.FileSystemStorage;
import org.reveno.atp.utils.MeasureUtils;

import java.io.File;
import java.util.Arrays;
import java.util.function.Consumer;

public class SnapshottingTest extends RevenoBaseTest {

    @Test
    public void testShutdownSnapshotting() throws Exception {
        Reveno reveno = createEngine();
        reveno.config().snapshotting().atShutdown(true);
        reveno.startup();

        generateAndSendCommands(reveno, 10_000);

        Assert.assertEquals(10_000, reveno.query().select(AccountView.class).size());
        Assert.assertEquals(10_000, reveno.query().select(OrderView.class).size());

        reveno.shutdown();

        Arrays.asList(tempDir.listFiles((dir, name) -> !(name.startsWith("snp")))).forEach(File::delete);

        reveno = createEngine();
        reveno.startup();

        Assert.assertEquals(10_000, reveno.query().select(AccountView.class).size());
        Assert.assertEquals(10_000, reveno.query().select(OrderView.class).size());

        reveno.shutdown();
    }

    @Test
    public void testSnapshottingEveryJavaSerializer() throws Exception {
        testSnapshottingEvery(new DefaultJavaSerializer());
    }

    @Test
    public void testSnapshottingEveryProtostuffSerializer() throws Exception {
        testSnapshottingEvery(new ProtostuffSerializer());
    }

    @Test
    public void testSnapshottingEveryInMemorySerializer() throws Exception {
        testSnapshottingEvery(null, new InMemorySnapshotter());
    }

    @Test
    public void testSnapshottingIntervalJavaSerializer() throws Exception {
        testSnapshottingInterval(new DefaultJavaSerializer());
    }

    @Test
    public void testSnapshottingIntervalProtostuffSerializer() throws Exception {
        testSnapshottingInterval(new ProtostuffSerializer());
    }

    public void testSnapshottingEvery(RepositoryDataSerializer repoSerializer) throws Exception {
        testSnapshottingEvery(repoSerializer, null);
    }

    public void testSnapshottingInterval(RepositoryDataSerializer repoSerializer) throws Exception {
        testSnapshottingInterval(repoSerializer, null);
    }

    public void testSnapshottingInterval(RepositoryDataSerializer repoSerializer, RepositorySnapshotter snapshotter) throws Exception {
        Consumer<TestRevenoEngine> consumer = r -> {
            r.domain().resetSnapshotters();
            if (snapshotter == null) {
                r.domain().snapshotWith(new DefaultSnapshotter(new FileSystemStorage(tempDir, new RevenoConfiguration.RevenoJournalingConfiguration()), repoSerializer))
                        .andRestoreWithIt();
            } else {
                r.domain().snapshotWith(snapshotter).andRestoreWithIt();
            }
        };
        Reveno reveno = createEngine(consumer);
        reveno.config().snapshotting().interval(1500);
        reveno.startup();

        generateAndSendCommands(reveno, 1_005);
        Assert.assertEquals(0, tempDir.listFiles((dir, name) -> name.startsWith("snp")).length);
        // yeah, this is really weird, should think about better approach
        Thread.sleep(2000);

        if (snapshotter == null) {
            Assert.assertTrue(tempDir.listFiles((dir, name) -> name.startsWith("snp")).length > 0);
        }
        reveno.shutdown();

        reveno = createEngine(consumer);
        reveno.startup();

        Assert.assertEquals(1_005, reveno.query().select(AccountView.class).size());
        Assert.assertEquals(1_005, reveno.query().select(OrderView.class).size());

        reveno.shutdown();
    }

    public void testSnapshottingEvery(RepositoryDataSerializer repoSerializer, RepositorySnapshotter snapshotter) throws Exception {
        Consumer<TestRevenoEngine> consumer = r -> {
            r.domain().resetSnapshotters();
            if (snapshotter == null) {
                r.domain().snapshotWith(new DefaultSnapshotter(new FileSystemStorage(tempDir, new RevenoConfiguration.RevenoJournalingConfiguration()), repoSerializer))
                        .andRestoreWithIt();
            } else {
                r.domain().snapshotWith(snapshotter).andRestoreWithIt();
            }
        };
        Reveno reveno = createEngine(consumer);
        reveno.config().snapshotting().atShutdown(false);
        reveno.config().snapshotting().every(1002);
        reveno.startup();

        generateAndSendCommands(reveno, 10_005);

        Assert.assertEquals(10_005, reveno.query().select(AccountView.class).size());
        Assert.assertEquals(10_005, reveno.query().select(OrderView.class).size());

        reveno.shutdown();

        if (snapshotter == null) {
            Assert.assertEquals(19, tempDir.listFiles((dir, name) -> name.startsWith("snp")).length);
        }

        reveno = createEngine(consumer);
        reveno.startup();

        Assert.assertEquals(10_005, reveno.query().select(AccountView.class).size());
        Assert.assertEquals(10_005, reveno.query().select(OrderView.class).size());

        generateAndSendCommands(reveno, 3);

        reveno.shutdown();

        if (snapshotter == null) {
            Arrays.asList(tempDir.listFiles((dir, name) -> name.startsWith("snp"))).forEach(File::delete);
        } else if (snapshotter instanceof InMemorySnapshotter) {
            ((InMemorySnapshotter)snapshotter).lastIdentifier = null;
            ((InMemorySnapshotter)snapshotter).lastJournalVersion = -1;
        }

        reveno = createEngine(consumer);
        Waiter accountCreatedEvent = listenFor(reveno, AccountCreatedEvent.class);
        Waiter orderCreatedEvent = listenFor(reveno, OrderCreatedEvent.class);
        reveno.startup();

        Assert.assertEquals(10_008, reveno.query().select(AccountView.class).size());
        Assert.assertEquals(10_008, reveno.query().select(OrderView.class).size());
        Assert.assertFalse(accountCreatedEvent.isArrived());
        Assert.assertFalse(orderCreatedEvent.isArrived());

        reveno.shutdown();
    }


    public static class InMemorySnapshotter implements RepositorySnapshotter {

        @Override
        public SnapshotIdentifier lastSnapshot() {
            return lastIdentifier;
        }

        @Override
        public long lastJournalVersionSnapshotted() {
            return lastJournalVersion;
        }

        @Override
        public SnapshotIdentifier prepare() {
            buffer.clear();
            return new DefaultSnapshotIdentifier();
        }

        @Override
        public void snapshot(RepositoryData repo, SnapshotIdentifier identifier) {
            serializer.serialize(repo, buffer);
        }

        @Override
        public void commit(long lastJournalVersion, SnapshotIdentifier identifier) {
            this.lastJournalVersion = lastJournalVersion;
            lastIdentifier = identifier;
        }

        @Override
        public RepositoryData load() {
            buffer.setReaderPosition(0);
            return serializer.deserialize(buffer);
        }

        protected Buffer buffer = new NettyBasedBuffer(false);
        protected volatile SnapshotIdentifier lastIdentifier;
        protected volatile long lastJournalVersion;
        protected ProtostuffSerializer serializer = new ProtostuffSerializer();

        private class DefaultSnapshotIdentifier implements SnapshotIdentifier {
            @Override
            public byte getType() {
                return 0x34;
            }

            @Override
            public long getTime() {
                return 0;
            }
        }
    }

}
