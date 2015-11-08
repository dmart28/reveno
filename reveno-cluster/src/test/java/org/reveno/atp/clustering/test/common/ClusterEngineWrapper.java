package org.reveno.atp.clustering.test.common;

import org.reveno.atp.clustering.api.InetAddress;
import org.reveno.atp.clustering.core.ClusterEngine;
import org.reveno.atp.clustering.core.api.StorageTransferServer;
import org.reveno.atp.clustering.core.buffer.ClusterProvider;
import org.reveno.atp.core.api.storage.FoldersStorage;
import org.reveno.atp.core.api.storage.JournalsStorage;
import org.reveno.atp.core.api.storage.SnapshotStorage;
import org.reveno.atp.core.storage.FileSystemStorage;

import java.io.File;

public class ClusterEngineWrapper extends ClusterEngine {
    public ClusterEngineWrapper(FoldersStorage foldersStorage, JournalsStorage journalsStorage, SnapshotStorage snapshotStorage, ClassLoader classLoader) {
        super(foldersStorage, journalsStorage, snapshotStorage, classLoader);
    }

    public ClusterEngineWrapper(String baseDir, ClassLoader classLoader) {
        super(baseDir, classLoader);
    }

    public ClusterEngineWrapper(File baseDir, ClassLoader classLoader) {
        super(baseDir, classLoader);
    }

    public ClusterEngineWrapper(File baseDir, ClassLoader classLoader, ClusterProvider factory) {
        super(baseDir, classLoader, factory);
    }

    public ClusterEngineWrapper(File baseDir) {
        super(baseDir);
    }

    public ClusterEngineWrapper(String baseDir, ClusterProvider factory) {
        super(baseDir, factory);
    }

    public ClusterEngineWrapper(String baseDir) {
        super(baseDir);
    }

    public ClusterEngineWrapper(FoldersStorage foldersStorage, JournalsStorage journalsStorage, SnapshotStorage snapshotStorage, ClassLoader classLoader, ClusterProvider factory) {
        super(foldersStorage, journalsStorage, snapshotStorage, classLoader, factory);
    }

    public ClusterEngineWrapper(FoldersStorage foldersStorage, JournalsStorage journalsStorage, SnapshotStorage snapshotStorage, ClassLoader classLoader, ClusterProvider factory, StorageTransferServer server) {
        super(foldersStorage, journalsStorage, snapshotStorage, classLoader, factory, server);
    }

    public File getBaseDir() {
        return ((FileSystemStorage) journalsStorage).getBaseDir();
    }

    public InetAddress getCurrentAddress() {
        return (InetAddress) configuration.currentNodeAddress();
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    protected int sequence;

}
