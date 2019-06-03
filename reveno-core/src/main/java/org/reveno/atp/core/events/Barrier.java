package org.reveno.atp.core.events;

/**
 * Barrier class used for denoting end of async
 * event execution flow of handlers. It is being put
 * at the end of the chain of async event handlers in sungle
 * executor, and if all previous tasks were executed successfully,
 * it sends async commit event to {@link EventPublisher}
 */
public class Barrier implements Runnable {
    protected final EventPublisher eventPublisher;
    private final boolean isReplay;
    private final long transactionId;
    private boolean isSuccessful = true;
    private boolean isOpen = false;

    public Barrier(EventPublisher eventPublisher, long transactionId, boolean isReplay) {
        this.eventPublisher = eventPublisher;
        this.transactionId = transactionId;
        this.isReplay = isReplay;
    }

    @Override
    public void run() {
        if (!isSuccessful) {
            eventPublisher.commitAsyncError(isReplay, transactionId);
        }
    }

    public void open() {
        isOpen = true;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void fail() {
        isSuccessful = false;
    }

}
