package org.reveno.atp.clustering.core.components;

import org.reveno.atp.clustering.api.SyncMode;
import org.reveno.atp.clustering.core.RevenoClusterConfiguration;
import org.reveno.atp.clustering.util.Utils;
import org.reveno.atp.commons.NamedThreadFactory;
import org.reveno.atp.core.api.storage.JournalsStorage;
import org.reveno.atp.core.storage.FileSystemStorage;
import org.reveno.atp.utils.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileStorageTransferServer {

    public void startup() {
        mainListener.execute(() -> {
            InetSocketAddress listenAddr =  new InetSocketAddress(config.revenoSync().port());
            ServerSocketChannel listener;
            try {
                listener = listen(listenAddr);
            } catch (IOException e) {
                LOG.error("SYNC: Failed to open server socket.", e);
                return;
            }
            LOG.debug("Transfer Server is started on {}", listenAddr);
            while (!Thread.interrupted()) {
                final SocketChannel conn = accept(listener);
                executor.execute(() -> sendStoragesToNode(conn));
            }
        });
    }

    protected void sendStoragesToNode(SocketChannel conn) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(10);
            if (waitForData(conn, buffer)) {
                buffer.rewind();
                if (config.revenoSync().mode() == SyncMode.SNAPSHOT) {
                    transfer(conn, storage.getLastSnapshotStore().getSnapshotPath());
                } else {
                    byte transferType = buffer.get();
                    if (transferType != StorageTransferModelSync.TRANSACTIONS
                            && transferType != StorageTransferModelSync.EVENTS) {
                        throw new IllegalArgumentException(String.format("Unknown transfer type %s", transferType));
                    }
                    long transactionId = buffer.getLong();
                    select(transactionId).stream().map(s -> {
                        if (transferType == StorageTransferModelSync.TRANSACTIONS)
                            return s.getTransactionCommitsAddress();
                        else return s.getEventsCommitsAddress();
                    }).forEach(a -> transfer(conn, a));
                }
            } else {
                LOG.debug("Can't receive data from {}", conn.getRemoteAddress());
            }
        } catch (IOException e) {
            LOG.error("SYNC: Failed to accept incoming connection", e);
        } finally {
            LOG.info("Closing transfer server connection.");
            close(conn);
        }
    }

    public void shutdown() {
        mainListener.shutdown();
        executor.shutdown();
    }

    protected void transfer(SocketChannel conn, String path) {
        try {
            File file = new File(storage.getBaseDir(), path);
            LOG.debug("Transfering {} to {}", file, conn.getRemoteAddress());
            FileChannel fc = new FileInputStream(file).getChannel();
            fc.transferTo(0, fc.size(), conn);
            fc.close();
        } catch (Exception e) {
            throw Exceptions.runtime(e);
        }
    }

    protected boolean waitForData(SocketChannel conn, ByteBuffer buffer) throws IOException {
        short[] bytesread = { 0 };
        return Utils.waitFor(() -> {
            int read = readSilent(conn, buffer);
            if (read != -1)
                bytesread[0] += read;
            return bytesread[0] == 10;
        }, config.revenoTimeouts().syncTimeout());
    }

    protected int readSilent(SocketChannel conn, ByteBuffer buffer) {
        try {
            return conn.read(buffer);
        } catch (IOException e) {
            throw Exceptions.runtime(e);
        }
    }

    protected SocketChannel accept(ServerSocketChannel listener) {
        try {
            SocketChannel conn = listener.accept();
            conn.configureBlocking(true);
            return conn;
        } catch (Exception e) {
            throw Exceptions.runtime(e);
        }
    }

    protected void close(SocketChannel channel) {
        try {
            channel.close();
        } catch (Exception e) {
            throw Exceptions.runtime(e);
        }
    }

    protected ServerSocketChannel listen(InetSocketAddress listenAddr) throws IOException {
        ServerSocketChannel listener;
        listener = ServerSocketChannel.open();
        ServerSocket ss = listener.socket();
        ss.setReuseAddress(true);
        ss.bind(listenAddr);
        return listener;
    }

    protected Set<JournalsStorage.JournalStore> select(long transactionId) {
        JournalsStorage.JournalStore[] stores = storage.getAllStores();
        Set<JournalsStorage.JournalStore> selected = new LinkedHashSet<>();
        for (int i = 0; i < stores.length; i++) {
            if (stores[i].getLastTransactionId() > transactionId) {
                if (i - 1 > 0) {
                    selected.add(stores[i - 1]);
                    selected.add(stores[i]);
                } else {
                    selected.add(stores[i]);
                }
            } else if (stores[i].getLastTransactionId() == transactionId) {
                selected.add(stores[i]);
            }
        }
        return selected;
    }


    /**
     * Protocol:
     * 1st byte - SyncMode
     * 2nd byte - 1b - Transactions, 2b - Events
     * 3-10 bytes - from transaction id
     *
     * rest - payload
     */
    public FileStorageTransferServer(RevenoClusterConfiguration config, FileSystemStorage storage) {
        this.config = config;
        this.storage = storage;
        this.executor = Executors.newFixedThreadPool(config.revenoSync().threadPoolSize(), new NamedThreadFactory("stf"));
        this.mainListener = Executors.newSingleThreadExecutor(new NamedThreadFactory("stf-main"));
    }

    protected RevenoClusterConfiguration config;
    protected FileSystemStorage storage;
    protected ExecutorService mainListener;
    protected ExecutorService executor;

    protected static final Logger LOG = LoggerFactory.getLogger(FileStorageTransferServer.class);
}
