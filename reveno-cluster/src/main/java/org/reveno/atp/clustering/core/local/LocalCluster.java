package org.reveno.atp.clustering.core.local;

import org.reveno.atp.clustering.api.*;
import org.reveno.atp.clustering.api.message.Marshaller;
import org.reveno.atp.clustering.api.message.Message;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class LocalCluster implements Cluster {



    @Override
    public void connect() {

    }

    @Override
    public void disconnect() {

    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public ClusterConnector gateway() {
        return null;
    }

    @Override
    public void marshallWith(Class<? extends Message> msgType, Marshaller marshaller) {

    }

    @Override
    public void listenEvents(Consumer<ClusterEvent> consumer) {

    }

    @Override
    public ClusterView view() {
        return null;
    }

    protected class LocalClusterConnector implements ClusterConnector {

        @Override
        public CompletableFuture<Boolean> send(List<Address> dest, Message message) {
            return null;
        }

        @Override
        public CompletableFuture<Boolean> send(List<Address> dest, Message message, Set<Flag> flags) {
            return null;
        }

        @Override
        public <T extends Message> void receive(int type, Consumer<T> consumer) {

        }

        @Override
        public <T extends Message> void receive(int type, Predicate<T> filter, Consumer<T> consumer) {

        }

        @Override
        public <T extends Message> void unsubscribe(int type, Consumer<T> consumer) {

        }
    }

}
