package org.reveno.atp.core;

import org.reveno.atp.core.api.FailoverManager;
import org.reveno.atp.core.api.channel.Buffer;

import java.util.function.Consumer;

public class UnclusteredFailoverManager implements FailoverManager {

    @Override
    public boolean isSingleNode() {
        return true;
    }

    @Override
    public boolean isMaster() {
        return true;
    }

    @Override
    public boolean isBlocked() {
        return false;
    }

    @Override
    public void onReplicationMessage(Consumer<Buffer> failoverHandler) {
    }

    @Override
    public void addOnBlocked(Runnable handler) {
    }

    @Override
    public void addOnUnblocked(Runnable handler) {
    }

    @Override
    public boolean replicate(Consumer<Buffer> bufferWriter) {
        return true;
    }

    @Override
    public long unprocessedCount() {
        return 0;
    }

    @Override
    public void processPendingMessages() {
    }
}
