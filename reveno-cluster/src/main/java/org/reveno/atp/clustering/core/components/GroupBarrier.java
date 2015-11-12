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

    public boolean waitOn() {
        boolean result = waitOnGroup();
        cluster.gateway().unsubscribe(BarrierPassed.TYPE, this::tryPassed);
        cluster.gateway().unsubscribe(BarrierMessage.TYPE, this::tryCame);
        return result;
    }

    private boolean waitOnGroup() {
        long start = System.nanoTime();
        Optional<Boolean> came = Optional.empty();
        while (!came.isPresent() && isSynced() && view.members().size() > 0) {
            ackAll();
            came = waitFor(cameBarrier, came);
            if (!came.isPresent() && allCame())
                came = Optional.of(true);
            if (Math.abs(System.nanoTime() - start) > timeoutNanos) {
                return false;
            }
        }
        ackAll(); // double ack
        if (came.isPresent()) {
            if (came.get()) {
                Optional<Boolean> passed = Optional.empty();
                while (!passed.isPresent() && isSynced()) {
                    ackPassed();
                    passed = waitFor(passedBarrier, passed);
                    if (!passed.isPresent() && allPassed())
                        passed = Optional.of(true);
                    if (Math.abs(System.nanoTime() - start) > timeoutNanos) {
                        return false;
                    }
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
            barrier.awaitNanos(WAIT_TIMEOUT_NANOS);
        } catch (InterruptedException e) {
            throw Exceptions.runtime(e);
        }
        if (!isSynced())
            came = Optional.of(false);
        return came;
    }

    public GroupBarrier(Cluster cluster, ClusterView view, String name, long timeoutNanos) {
        this.cluster = cluster;
        this.view = view;
        this.id = name + view.viewId();
        this.timeoutNanos = timeoutNanos;

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
        return came.containsAll(view.members());
    }

    protected boolean allPassed() {
        return passed.containsAll(view.members());
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
    protected long timeoutNanos;

    protected Set<Address> came = Collections.newSetFromMap(new ConcurrentHashMap<>());
    protected Set<Address> passed = Collections.newSetFromMap(new ConcurrentHashMap<>());

    protected SignalBarrier cameBarrier = new SignalBarrier();
    protected SignalBarrier passedBarrier = new SignalBarrier();

    protected static final long WAIT_TIMEOUT_NANOS = 200_000;
}