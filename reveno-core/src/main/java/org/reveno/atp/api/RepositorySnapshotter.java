package org.reveno.atp.api;

import org.reveno.atp.api.domain.RepositoryData;

public interface RepositorySnapshotter {

    default boolean hasAny() {
        return lastSnapshot() != null;
    }

    /**
     * Returns last available snapshot.
     *
     * @return
     */
    SnapshotIdentifier lastSnapshot();

    /**
     * Returns last version of Journal, after which snapshot was made. It was
     * provided on the subsequent {@link #commit(long, SnapshotIdentifier)} method.
     * <p>
     * It should return -1 if no snapshot had been made yet by this snapshotter.
     *
     * @return
     */
    long lastJournalVersionSnapshotted();

    /**
     * Prepares SnapshotIdentifier pointer, using which snapshot
     * can be written in particular way. It should be noted that this identifier
     * should point for some temporary place, so, in case {@link #commit(long, SnapshotIdentifier)} is
     * never called, this snapshot wouldn't affect engine replays at all, in other words, not used
     * at all.
     *
     * @return
     */
    SnapshotIdentifier prepare();

    /**
     * Performs actual snapshotting of {@link RepositoryData}. It still won't be available to
     * the system for replay until {@link #commit(long, SnapshotIdentifier)} method is called.
     *
     * @param repo       latest state of domain model
     * @param identifier the result of previously called {@link #prepare()} method call
     */
    void snapshot(RepositoryData repo, SnapshotIdentifier identifier);

    /**
     * Commits snapshot @{code identifier} - makes it available for engine to replay.
     *
     * @param identifier
     */
    void commit(long lastJournalVersion, SnapshotIdentifier identifier);

    /**
     * Loads last snapshot into {@link RepositoryData}
     *
     * @return snapshotted state of domain model
     */
    RepositoryData load();


    interface SnapshotIdentifier {
        byte getType();

        long getTime();
    }

}
