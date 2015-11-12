/**
 *  Copyright (c) 2015 The original author or authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */


package org.reveno.atp.clustering.core;

import org.reveno.atp.clustering.api.ClusterBuffer;
import org.reveno.atp.core.api.FailoverManager;
import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.api.serialization.TransactionInfoSerializer;
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

    /**
     * Must always be called by single thread only.
     * @param cmds
     * @return
     */
    public boolean newMessage(List<Object> cmds) {
        if (!isBlocked && failoverHandler != null) {
            if (unprocessedCount() != 0) {
                processPendingMessages();
            }
            failoverHandler.accept(cmds);
            return true;
        } else {
            // have to use lock here since "unblock" and "newMessage" are handled
            // by independed threads which possible might introduce concurrency here
            synchronized (this) {
                notProcessedTransactions.add(cmds);
            }
            return false;
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
            isBlocked = false;
            processPendingMessages();
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
    public void onReplicationMessage(Consumer<List<Object>> failoverHandler) {
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
            buffer.prepare();
            bufferWriter.accept(buffer);

            return buffer.replicate();
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public long unprocessedCount() {
        return notProcessedTransactions.size();
    }

    @Override
    public synchronized void processPendingMessages() {
        if (notProcessedTransactions.size() > 0) {
            notProcessedTransactions.forEach(failoverHandler::accept);
            notProcessedTransactions.clear();
        }
    }

    public void setMaster(boolean isMaster) {
        this.isMaster = isMaster;
    }


    public ClusterFailoverManager(TransactionInfoSerializer serializer, ClusterBuffer buffer) {
        this.buffer = buffer;
        buffer.messageNotifier(serializer, this::newMessage);
    }

    protected ClusterBuffer buffer;
    protected List<Runnable> onBlockedListeners = new CopyOnWriteArrayList<>();
    protected List<Runnable> onUnblockedListeners = new CopyOnWriteArrayList<>();
    protected volatile Consumer<List<Object>>  failoverHandler;
    protected volatile boolean isMaster, isBlocked;

    protected List<List<Object>> notProcessedTransactions = new CopyOnWriteArrayList<>();

    protected static final Logger LOG = LoggerFactory.getLogger(ClusterFailoverManager.class);
}
