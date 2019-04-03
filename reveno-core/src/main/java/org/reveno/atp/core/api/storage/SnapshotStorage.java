package org.reveno.atp.core.api.storage;

import org.reveno.atp.api.RepositorySnapshotter.SnapshotIdentifier;
import org.reveno.atp.core.api.channel.Channel;

public interface SnapshotStorage {

    Channel snapshotChannel(String address);

    SnapshotStore getLastSnapshotStore();

    SnapshotStore nextSnapshotAfter(long lastJournalVersion);

    SnapshotStore nextTempSnapshotStore();

    void removeSnapshotStore(SnapshotStore snapshot);

    void move(SnapshotStore from, SnapshotStore to);

    void removeLastSnapshotStore();


    class SnapshotStore implements SnapshotIdentifier {
        public static final byte TYPE = 0x1;
        private String snapshotPath;
        private long snapshotTime;
        private long version;
        private long lastJournalVersion;

        public SnapshotStore(String path, long time, long version, long lastJournalVersion) {
            this.snapshotPath = path;
            this.snapshotTime = time;
            this.version = version;
            this.lastJournalVersion = lastJournalVersion;
        }

        public byte getType() {
            return TYPE;
        }

        public String getSnapshotPath() {
            return snapshotPath;
        }

        public void setSnapshotPath(String snapshotPath) {
            this.snapshotPath = snapshotPath;
        }

        public long getTime() {
            return snapshotTime;
        }

        public long getVersion() {
            return version;
        }

        public long getLastJournalVersion() {
            return lastJournalVersion;
        }

        @Override
        public String toString() {
            return String.format("[store:%s]", snapshotPath);
        }
    }

}
