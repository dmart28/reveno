package org.reveno.atp.clustering.core.jgroups;

import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;

import java.util.LinkedList;
import java.util.List;
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
        receivers.forEach(r -> r.accept(msg));
    }

    @Override
    public void viewAccepted(View view) {
        viewAcceptors.forEach(a -> a.accept(view));
    }

    protected List<Consumer<Message>> receivers = new LinkedList<>();
    protected List<Consumer<View>> viewAcceptors = new LinkedList<>();
}
