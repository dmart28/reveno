package org.reveno.atp.clustering.core;

import org.reveno.atp.clustering.api.ClusterConfiguration;
import org.reveno.atp.core.Engine;
import org.reveno.atp.core.api.storage.FoldersStorage;
import org.reveno.atp.core.api.storage.JournalsStorage;
import org.reveno.atp.core.api.storage.SnapshotStorage;

import java.io.File;

public class ClusterEngine extends Engine {
    public ClusterEngine(FoldersStorage foldersStorage, JournalsStorage journalsStorage, SnapshotStorage snapshotStorage,
                         ClassLoader classLoader) {
        super(foldersStorage, journalsStorage, snapshotStorage, classLoader);
    }

    public ClusterEngine(String baseDir, ClassLoader classLoader) {
        super(baseDir, classLoader);
    }

    public ClusterEngine(File baseDir, ClassLoader classLoader) {
        super(baseDir, classLoader);
    }

    public ClusterEngine(File baseDir) {
        super(baseDir);
    }

    public ClusterEngine(String baseDir) {
        super(baseDir);
    }

    public ClusterConfiguration clusterConfiguration() {
        return configuration;
    }

    @Override
    public void startup() {
        init();
        connectSystemHandlers();

        // TODO don't forget about quorum logic!
    }

    @Override
    public void shutdown() {

    }

    protected RevenoClusterConfiguration configuration = new RevenoClusterConfiguration();

}
