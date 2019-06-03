package org.reveno.atp.core.snapshots;

import org.reveno.atp.api.RepositorySnapshotter;
import org.reveno.atp.api.domain.RepositoryData;
import org.reveno.atp.core.api.channel.Channel;
import org.reveno.atp.core.api.serialization.RepositoryDataSerializer;
import org.reveno.atp.core.api.storage.SnapshotStorage;
import org.reveno.atp.core.api.storage.SnapshotStorage.SnapshotStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultSnapshotter implements RepositorySnapshotter {
    protected static final Logger LOG = LoggerFactory.getLogger(DefaultSnapshotter.class);
    protected final SnapshotStorage storage;
    protected final RepositoryDataSerializer[] serializers;

    public DefaultSnapshotter(
            SnapshotStorage storage,
            RepositoryDataSerializer... serializers) {
        this.storage = storage;
        this.serializers = serializers;
    }

    @Override
    public SnapshotIdentifier lastSnapshot() {
        return storage.getLastSnapshotStore();
    }

    @Override
    public long lastJournalVersionSnapshotted() {
        SnapshotStore store = storage.getLastSnapshotStore();
        return store == null ? -1 : store.getLastJournalVersion();
    }

    @Override
    public SnapshotIdentifier prepare() {
        SnapshotIdentifier si = storage.nextTempSnapshotStore();
        LOG.debug("Prepared default snapshot {}", si);
        return si;
    }

    @Override
    public void commit(long lastJournalVersion, SnapshotIdentifier identifier) {
        if (identifier.getType() != SnapshotStore.TYPE) {
            LOG.error("Wrong snapshot identifier type!");
            return;
        }
        SnapshotStore snap = (SnapshotStore) identifier;
        storage.move(snap, storage.nextSnapshotAfter(lastJournalVersion));
        LOG.debug("Moved default repository snapshot {}", snap);
    }

    @Override
    public void snapshot(RepositoryData repo, SnapshotIdentifier identifier) {
        if (identifier.getType() != SnapshotStore.TYPE) {
            LOG.error("Wrong snapshot identifier type!");
            return;
        }
        SnapshotStore snap = (SnapshotStore) identifier;
        for (int i = 0; i < serializers.length; i++) {
            try (Channel c = storage.snapshotChannel(snap.getSnapshotPath())) {
                LOG.debug("Performing default repository snapshot to {}", snap);

                final int index = i;
                c.write(b -> serializers[index].serialize(repo, b), true);
            } catch (Throwable t) {
                if (i + 1 == serializers.length) {
                    throw new RuntimeException(t);
                } else {
                    LOG.info("Can't snapshot with {}, falling back to {}", serializers[i].getClass().getSimpleName(),
                            serializers[i + 1].getClass().getSimpleName());
                    storage.removeSnapshotStore(snap);
                    SnapshotStore nextStore = storage.nextTempSnapshotStore();
                    snap.setSnapshotPath(nextStore.getSnapshotPath());
                }
                continue;
            }
            break;
        }
    }

    @Override
    public RepositoryData load() {
        if (storage.getLastSnapshotStore() == null)
            return null;

        SnapshotStore snap = storage.getLastSnapshotStore();
        for (RepositoryDataSerializer serializer : serializers) {
            try (Channel c = storage.snapshotChannel(snap.getSnapshotPath())) {
                LOG.debug("Loading repository snapshot from {}", snap);

                return serializer.deserialize(c.read());
            } catch (Throwable ignored) {
            } finally {
                LOG.debug("Loaded repository snapshot from {}", snap);
            }
        }
        return null;
    }

}
