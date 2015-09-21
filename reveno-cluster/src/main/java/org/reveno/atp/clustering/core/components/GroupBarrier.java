package org.reveno.atp.clustering.core.components;

import org.reveno.atp.clustering.api.Address;
import org.reveno.atp.clustering.api.Cluster;
import org.reveno.atp.clustering.api.ClusterView;
import org.reveno.atp.clustering.core.messages.BarrierMessage;
import org.reveno.atp.clustering.core.messages.BarrierPassed;
import org.reveno.atp.utils.Exceptions;
import org.reveno.atp.utils.SignalBarrier;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cluster barrier used to wait until all nodes
 * in particular View came into a single point.
 *
 * After waitFor method execution, the instance of
 * this class can't be reused.
 */
public class GroupBarrier {

    public boolean waitFor() {
        boolean result = waitOnGroup();
        cluster.gateway().unsubscribe(BarrierPassed.TYPE, this::tryPassed);
        cluster.gateway().unsubscribe(BarrierMessage.TYPE, this::tryCame);
        return result;
    }

    private boolean waitOnGroup() {
        Optional<Boolean> came = Optional.empty();
        while (!came.isPresent() && isSynced() && view.members().size() > 0) {
            ackAll();
            came = waitFor(cameBarrier, came);
            if (!came.isPresent() && allCame())
                came = Optional.of(true);
        }
        ackAll(); // double ack
        if (came.isPresent()) {
            if (came.get()) {
                Optional<Boolean> passed = Optional.empty();
                while (!passed.isPresent() && isSynced()) {
                    ackPassed();
                    passed = waitFor(passedBarrier, passed);
                    if (!came.isPresent() && allPassed())
                        passed = Optional.of(true);
                }
                return passed.isPresent() && passed.get();
            } else
                return false;
        } else {
            return !(view.members().size() > 0);
        }
    }

    protected Optional<Boolean> waitFor(SignalBarrier barrier, Optional<Boolean> came) {
        try {
            barrier.awaitNanos(WAIT_TIMEOUT);
        } catch (InterruptedException e) {
            Exceptions.runtime(e);
        }
        if (!isSynced())
            came = Optional.of(false);
        return came;
    }

    public GroupBarrier(Cluster cluster, ClusterView view, String name) {
        this.cluster = cluster;
        this.view = view;
        this.id = name + view.viewId();

        cluster.gateway().receive(BarrierMessage.TYPE, m -> m.id.equals(id) && isSynced(), this::tryCame);
        cluster.gateway().receive(BarrierPassed.TYPE, m -> m.id.equals(id) && isSynced(), this::tryPassed);
    }

    protected void tryPassed(BarrierPassed m) {
        passed.add(m.address());
        if (allPassed()) {
            passedBarrier.signal();
        }
    }

    protected void tryCame(BarrierMessage m) {
        came.add(m.address());
        if (allCame()) {
            cameBarrier.signal();
        }
    }

    protected boolean allCame() {
        return came.equals(view.members());
    }

    protected boolean allPassed() {
        return passed.equals(view.members());
    }

    protected void ackAll() {
        cluster.gateway().send(view.members(), new BarrierMessage(id));
    }

    protected void ackPassed() {
        cluster.gateway().send(view.members(), new BarrierPassed(id));
    }

    protected boolean isSynced() {
        return cluster.isConnected() && cluster.view().viewId() == view.viewId();
    }

    protected Cluster cluster;
    protected ClusterView view;
    protected String id;

    protected Set<Address> came = Collections.newSetFromMap(new ConcurrentHashMap<>());
    protected Set<Address> passed = Collections.newSetFromMap(new ConcurrentHashMap<>());

    protected SignalBarrier cameBarrier = new SignalBarrier();
    protected SignalBarrier passedBarrier = new SignalBarrier();

    protected static final long WAIT_TIMEOUT = 1_000_000;
}