package org.reveno.atp.core.events;

import org.reveno.atp.api.EventsManager.EventMetadata;
import org.reveno.atp.core.api.Destroyable;

import java.util.concurrent.CompletableFuture;

public class Event implements Destroyable {
    private boolean isAborted;
    private boolean isReplay;
    private boolean isReplicated;
    private long flag;
    private CompletableFuture<?> syncFuture;
    private long transactionId;
    private Object[] events;
    private EventMetadata eventMetadata;

    public boolean isAborted() {
        return isAborted;
    }

    public boolean isReplay() {
        return isReplay;
    }

    public boolean isReplicated() {
        return isReplicated;
    }

    public long getFlag() {
        return flag;
    }

    public CompletableFuture<?> syncFuture() {
        return syncFuture;
    }

    public long transactionId() {
        return transactionId;
    }

    public Object[] events() {
        return events;
    }

    public EventMetadata eventMetadata() {
        return eventMetadata;
    }

    public Event replay(boolean isReplay) {
        this.isReplay = isReplay;
        return this;
    }

    public void abort() {
        this.isAborted = true;
    }


    public Event replicate() {
        this.isReplicated = true;
        return this;
    }

    public Event flag(long flag) {
        this.flag = flag;
        return this;
    }

    public Event syncFuture(CompletableFuture<?> syncFuture) {
        this.syncFuture = syncFuture;
        return this;
    }

    public Event transactionId(long transactionId) {
        this.transactionId = transactionId;
        return this;
    }

    public Event events(Object[] events) {
        this.events = events;
        return this;
    }

    public Event eventMetadata(EventMetadata eventMetadata) {
        this.eventMetadata = eventMetadata;
        return this;
    }

    public Event reset() {
        isAborted = false;
        isReplay = false;
        isReplicated = false;
        flag = 0;
        transactionId = 0L;
        syncFuture = null;
        events = null;
        eventMetadata = null;

        return this;
    }

    public void destroy() {
        reset();
    }

}
