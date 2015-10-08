package org.reveno.atp.clustering.core;

import org.reveno.atp.clustering.api.ClusterBuffer;
import org.reveno.atp.core.api.FailoverManager;
import org.reveno.atp.core.api.channel.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.LongStream;

public class ClusterFailoverManager implements FailoverManager {

    public ClusterBuffer buffer() {
        return buffer;
    }

    public void newMessage() {
        if (!isBlocked && failoverHandler != null) {
            failoverHandler.accept(buffer);
        } else {
            notProcessed.incrementAndGet();
        }
    }

    public synchronized void block() {
        if (!isBlocked) {
            isBlocked = true;
            onBlockedListeners.forEach(Runnable::run);
        } else {
            throw new IllegalArgumentException("Failover manager is already blocked.");
        }
    }

    public synchronized void unblock() {
        if (isBlocked) {
            onUnblockedListeners.forEach(Runnable::run);
            processPendingMessages();
            isBlocked = false;
        } else {
            throw new IllegalArgumentException("Failover manager is not blocked.");
        }
    }

    @Override
    public boolean isMaster() {
        return isMaster;
    }

    @Override
    public boolean isBlocked() {
        return isBlocked;
    }

    @Override
    public void onReplicationMessage(Consumer<Buffer> failoverHandler) {
        this.failoverHandler = failoverHandler;
    }

    @Override
    public void addOnBlocked(Runnable handler) {
        onBlockedListeners.add(handler);
    }

    @Override
    public void addOnUnblocked(Runnable handler) {
        onUnblockedListeners.add(handler);
    }

    @Override
    public boolean replicate(Consumer<Buffer> bufferWriter) {
        try {
            buffer.markSize();
            bufferWriter.accept(buffer);
            buffer.writeSize();

            return buffer.replicate();
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void processPendingMessages() {
        long unprocessed;
        while (!notProcessed.compareAndSet((unprocessed = notProcessed.get()), 0)) {
        }
        LongStream.of(unprocessed).forEach(l -> failoverHandler.accept(buffer));
    }

    public void setMaster(boolean isMaster) {
        this.isMaster = isMaster;
    }


    public ClusterFailoverManager(ClusterBuffer buffer) {
        this.buffer = buffer;
        buffer.messageNotifier(this::newMessage);
    }

    protected ClusterBuffer buffer;
    protected List<Runnable> onBlockedListeners = new CopyOnWriteArrayList<>();
    protected List<Runnable> onUnblockedListeners = new CopyOnWriteArrayList<>();
    protected volatile Consumer<Buffer> failoverHandler;
    protected volatile boolean isMaster, isBlocked;

    protected volatile AtomicLong notProcessed = new AtomicLong(0);

    protected static final Logger LOG = LoggerFactory.getLogger(ClusterFailoverManager.class);
}
