package org.reveno.atp.utils;

/* I release this code into the public domain.
 * http://unlicense.org/UNLICENSE
 */

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.LockSupport;

/**
 * A simple barrier for awaiting a signal.
 * Only one thread at a time may await the signal.
 */
public class SignalBarrier {
    /**
     * The Thread that is currently awaiting the signal.
     * !!! Don't call this directly !!!
     */
    @SuppressWarnings("unused")
    private volatile Thread _owner;

    /** Used to update the owner atomically */
    private static final AtomicReferenceFieldUpdater<SignalBarrier, Thread> ownerAccess =
            AtomicReferenceFieldUpdater.newUpdater(SignalBarrier.class, Thread.class, "_owner");

    /** Create a new SignalBarrier without an owner. */
    public SignalBarrier() {
        _owner = null;
    }

    /**
     * Signal the owner that the barrier is ready.
     * This has no effect if the SignalBarrer is unowned.
     */
    public void signal() {
        // Remove the current owner of this barrier.
        Thread t = ownerAccess.getAndSet(this, null);

        // If the owner wasn't null, unpark it.
        if (t != null) {
            LockSupport.unpark(t);
        }
    }

    /**
     * Claim the SignalBarrier and block until signaled.
     *
     * @throws IllegalStateException If the SignalBarrier already has an owner.
     * @throws InterruptedException If the thread is interrupted while waiting.
     */
    public void await() throws InterruptedException {
        // Get the thread that would like to await the signal.
        Thread t = Thread.currentThread();

        // If a thread is attempting to await, the current owner should be null.
        if (!ownerAccess.compareAndSet(this, null, t)) {
            throw new IllegalStateException("A second thread tried to acquire a signal barrier that is already owned.");
        }

        // The current thread has taken ownership of this barrier.
        // Park the current thread until the signal. Record this
        // signal barrier as the 'blocker'.
        LockSupport.park(this);
        // If a thread has called #signal() the owner should already be null.
        // However the documentation for LockSupport.unpark makes it clear that
        // threads can wake up for absolutely no reason. Do a compare and set
        // to make sure we don't wipe out a new owner, keeping in mind that only
        // thread should be awaiting at any given moment!
        ownerAccess.compareAndSet(this, t, null);

        // Check to see if we've been unparked because of a thread interrupt.
        if (t.isInterrupted())
            throw new InterruptedException();
    }

    /**
     * Claim the SignalBarrier and block until signaled or the timeout expires.
     *
     * @throws IllegalStateException If the SignalBarrier already has an owner.
     * @throws InterruptedException If the thread is interrupted while waiting.
     *
     * @param timeout The timeout duration in nanoseconds.
     * @return The timeout minus the number of nanoseconds that passed while waiting.
     */
    public long awaitNanos(long timeout) throws InterruptedException {
        if (timeout <= 0)
            return 0;
        // Get the thread that would like to await the signal.
        Thread t = Thread.currentThread();

        // If a thread is attempting to await, the current owner should be null.
        if (!ownerAccess.compareAndSet(this, null, t)) {
            throw new IllegalStateException("A second thread tried to acquire a signal barrier is already owned.");
        }

        // The current thread owns this barrier.
        // Park the current thread until the signal. Record this
        // signal barrier as the 'blocker'.
        // Time the park.
        long start = System.nanoTime();
        LockSupport.parkNanos(this, timeout);
        ownerAccess.compareAndSet(this, t, null);
        long stop = System.nanoTime();

        // Check to see if we've been unparked because of a thread interrupt.
        if (t.isInterrupted())
            throw new InterruptedException();

        // Return the number of nanoseconds left in the timeout after what we
        // just waited.
        return Math.max(timeout - stop + start, 0L);
    }
}