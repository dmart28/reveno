package org.reveno.atp.clustering.core.components;

import org.reveno.atp.clustering.core.RevenoClusterConfiguration;
import org.reveno.atp.commons.NamedThreadFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class StorageTransferServer {

    public void startup() {

    }

    public void shutdown() {

    }


    /**
     * Protocol:
     * 1st byte - SyncMode
     * 2nd byte - Transactions or Events
     *
     * rest - payload
     */
    public StorageTransferServer(RevenoClusterConfiguration config) {
        this.config = config;
        this.executor = Executors.newFixedThreadPool(config.syncThreadPoolSize(), new NamedThreadFactory("stf"));
    }

    protected RevenoClusterConfiguration config;
    protected Executor executor;
}
