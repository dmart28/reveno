package org.reveno.atp.clustering.core.jgroups;

import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.reveno.atp.utils.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class JChannelReceiver extends ReceiverAdapter {

    public void addReceiver(Consumer<Message> receiver) {
        this.receivers.add(receiver);
    }

    public void addViewAcceptor(Consumer<View> acceptor) {
        this.viewAcceptors.add(acceptor);
    }

    @Override
    public void receive(Message msg) {
        try {
            receivers.forEach(r -> r.accept(msg));
        } catch (Throwable t) {
            LOG.error(t.getMessage(), t);
            throw Exceptions.runtime(t);
        }
    }

    @Override
    public void viewAccepted(View view) {
        viewAcceptors.forEach(a -> a.accept(view));
    }

    protected List<Consumer<Message>> receivers = new CopyOnWriteArrayList<>();
    protected List<Consumer<View>> viewAcceptors = new CopyOnWriteArrayList<>();
    protected static final Logger LOG = LoggerFactory.getLogger(JChannelReceiver.class);
}
