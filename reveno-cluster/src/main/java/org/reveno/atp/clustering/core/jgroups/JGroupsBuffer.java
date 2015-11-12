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

import org.jgroups.Header;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.View;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.protocols.RSVP;
import org.reveno.atp.clustering.api.ClusterBuffer;
import org.reveno.atp.clustering.api.ClusterEvent;
import org.reveno.atp.clustering.api.ClusterView;
import org.reveno.atp.clustering.api.IOMode;
import org.reveno.atp.clustering.core.RevenoClusterConfiguration;
import org.reveno.atp.clustering.core.components.AbstractClusterBuffer;
import org.reveno.atp.clustering.util.Tuple;
import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.api.serialization.TransactionInfoSerializer;
import org.reveno.atp.core.channel.NettyBasedBuffer;
import org.reveno.atp.utils.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * JGroups implementation of {@link ClusterBuffer}. It is intended to be used mainly
 * for test and debug purposes, as it's not very efficient approach in low-latency world.
 */
public class JGroupsBuffer extends AbstractClusterBuffer implements ClusterBuffer {

    @Override
    public void connect() {
        if (isConnected) return;
        synchronized (ClassConfigurator.class) {
            if (ClassConfigurator.get(ClusterBufferHeader.ID) == null)
                ClassConfigurator.add(ClusterBufferHeader.ID, ClusterBufferHeader.class);
        }

        try {
            ((JChannelReceiver) channel.getReceiver()).addReceiver(msg -> { if (msg.getHeader(ClusterBufferHeader.ID) != null) {
                if (!isLocked) {
                    receiveBuffer.writeBytes(msg.getBuffer());
                    messageListener.accept(serializer.deserializeCommands(receiveBuffer));
                    receiveBuffer.clear();
                }
            }});
            ((JChannelReceiver) channel.getReceiver()).addViewAcceptor(this::rebuildAddresses);
        } catch (Exception e) {
            throw Exceptions.runtime(e);
        } finally {
            isConnected = true;
        }
    }

    @Override
    public void disconnect() {
        channel.disconnect();
    }

    @Override
    public void onView(ClusterView view) {
    }

    @Override
    public void messageNotifier(TransactionInfoSerializer serializer, Consumer<List<Object>> listener) {
        this.serializer = serializer;
        this.messageListener = listener;
    }

    @Override
    public void failoverNotifier(Consumer<ClusterEvent> listener) {
        // ignore since we don't catch failover exceptions here
        // but rather in JGroupsCluster (JGroupsBuffer is not supposed to be used
        // without it for now)
    }

    @Override
    public void lockIncoming() {
        isLocked = true;
    }

    @Override
    public void unlockIncoming() {
        isLocked = false;
    }

    @Override
    public void erase() {
    }

    @Override
    public void prepare() {
    }

    @Override
    public boolean replicate() {
        if (lastView == null || !channel.getView().equals(lastView)) {
            rebuildAddresses(channel.getView());
        }
        byte[] data = sendBuffer.readBytes(sendBuffer.length());
        try {
            addresses.forEach(p -> {
                if (restrictOn != null && p.mode != restrictOn) {
                    return;
                }
                org.jgroups.Message msg = new org.jgroups.Message(p.address, null, data);
                msg.setTransientFlag(Message.TransientFlag.DONT_LOOPBACK);
                msg.putHeader(ClusterBufferHeader.ID, new ClusterBufferHeader());
                if (p.mode == IOMode.ASYNC_UNRELIABLE)
                    msg.setFlag(Message.Flag.NO_RELIABILITY);
                try {
                    channel.send(msg);
                } catch (Exception e) {
                    LOG.error("replicate", e);
                    throw Exceptions.runtime(e);
                }
            });
        } catch (Exception e) {
            // TODO send as metric
            return false;
        } finally {
            sendBuffer.clear();
        }
        return true;
    }

    protected void rebuildAddresses(View view) {
        addresses = view.getMembers().stream()
                .map(a -> new Tuple<>(a, JChannelHelper.physicalAddress(channel, config, a)))
                .filter(t -> t.getVal2() != null)
                .filter(t -> config.clusterNodeAddresses().contains(t.getVal2()))
                .map(t -> new AddressPair(t.getVal1(), t.getVal2().getAddressType()))
                .sorted((a, b) -> {
                    if (a.mode == IOMode.ASYNC_UNRELIABLE) return 1; else return -1;
                })
                .collect(Collectors.toList());
        lastView = view;
        LOG.info("JGroups Buffer members: {}", addresses.size());
    }

    public JGroupsBuffer(RevenoClusterConfiguration config, JChannel channel) {
        this(config, channel, Optional.empty());
    }

    public JGroupsBuffer(RevenoClusterConfiguration config, JChannel channel, Optional<IOMode> restrictOn) {
        this.channel = channel;
        this.config = config;
        this.restrictOn = restrictOn.orElse(null);
    }

    protected JChannel channel;
    protected RevenoClusterConfiguration config;
    protected TransactionInfoSerializer serializer;
    protected IOMode restrictOn = null;

    protected volatile boolean isConnected = false;
    protected volatile boolean isLocked = false;
    protected volatile List<AddressPair> addresses = new ArrayList<>();
    protected volatile View lastView = null;
    protected Consumer<List<Object>> messageListener = l -> {};
    protected Buffer receiveBuffer = new NettyBasedBuffer();

    protected Logger LOG = LoggerFactory.getLogger(JGroupsBuffer.class);

    public static class ClusterBufferHeader extends Header {
        public static final short ID = 0xaac;

        @Override
        public int size() {
            return 0;
        }

        @Override
        public void writeTo(DataOutput out) throws Exception {
        }

        @Override
        public void readFrom(DataInput in) throws Exception {
        }
    }

    protected static class AddressPair {
        public final org.jgroups.Address address;
        public final IOMode mode;

        public AddressPair(org.jgroups.Address address, IOMode mode) {
            this.address = address;
            this.mode = mode;
        }
    }
}
