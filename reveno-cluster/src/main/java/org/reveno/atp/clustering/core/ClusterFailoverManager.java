package org.reveno.atp.clustering.core;

import org.reveno.atp.clustering.api.ClusterBuffer;
import org.reveno.atp.core.api.FailoverManager;
import org.reveno.atp.core.api.channel.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class ClusterFailoverManager implements FailoverManager {

    public void newMessage(Buffer message) {
        if (!isBlocked) {
            failoverHandler.accept(message);
        } else {
            notYetHandled = message;
            notProcessed.incrementAndGet();
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
            outBuffer.markSize();
            bufferWriter.accept(outBuffer);
            outBuffer.writeSize();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void processPendingMessages() {

    }

    public void setMaster(boolean isMaster) {
        this.isMaster = isMaster;
    }

    public void setBlocked(boolean isBlocked) {
        this.isBlocked = isBlocked;
    }


    public ClusterFailoverManager(ClusterBuffer outBuffer) {
        this.outBuffer = outBuffer;
    }

    protected ClusterBuffer outBuffer;
    protected List<Runnable> onBlockedListeners = new CopyOnWriteArrayList<>();
    protected List<Runnable> onUnblockedListeners = new CopyOnWriteArrayList<>();
    protected volatile Consumer<Buffer> failoverHandler;
    protected volatile boolean isMaster, isBlocked;

    protected volatile AtomicInteger notProcessed = new AtomicInteger(0);
    protected volatile Buffer notYetHandled;

    protected static final Logger LOG = LoggerFactory.getLogger(ClusterFailoverManager.class);
}
