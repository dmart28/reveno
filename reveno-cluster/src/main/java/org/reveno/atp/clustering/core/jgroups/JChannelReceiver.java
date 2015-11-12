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
        }
    }

    @Override
    public void viewAccepted(View view) {
        try {
            viewAcceptors.forEach(a -> a.accept(view));
        } catch (Throwable t) {
            LOG.error(t.getMessage(), t);
        }
    }

    protected List<Consumer<Message>> receivers = new CopyOnWriteArrayList<>();
    protected List<Consumer<View>> viewAcceptors = new CopyOnWriteArrayList<>();
    protected static final Logger LOG = LoggerFactory.getLogger(JChannelReceiver.class);
}
