package org.reveno.atp.clustering.core.jgroups;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jgroups.*;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.protocols.RSVP;
import org.reveno.atp.clustering.api.*;
import org.reveno.atp.clustering.api.Address;
import org.reveno.atp.clustering.api.message.Marshaller;
import org.reveno.atp.clustering.api.message.Message;
import org.reveno.atp.clustering.core.RevenoClusterConfiguration;
import org.reveno.atp.clustering.core.marshallers.JsonMarshaller;
import org.reveno.atp.clustering.util.Tuple;
import org.reveno.atp.core.channel.NettyBasedBuffer;
import org.reveno.atp.utils.Exceptions;
import org.reveno.atp.utils.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class JGroupsCluster implements Cluster {
    protected static final ClusterView DEFAULT_VIEW = new ClusterView(0, Collections.emptyList());

    @Override
    public void connect() {
        if (isConnected) return;
        if (ClassConfigurator.get(ClusterMessageHeader.ID) == null)
            ClassConfigurator.add(ClusterMessageHeader.ID, ClusterMessageHeader.class);

        try {
            ((JChannelReceiver) channel.getReceiver()).addReceiver(msg -> { if (msg.getHeader(ClusterMessageHeader.ID) != null) {
                NettyBasedBuffer buffer = new NettyBasedBuffer(msg.getLength(), msg.getLength(), false);
                buffer.writeBytes(msg.getBuffer());
                Message message = marshaller.unmarshall(buffer);
                message.address(JChannelHelper.physicalAddress(channel, config, msg.getSrc()));
                //LOG.info(config.currentNodeAddress() + " : " + message.toString());

                receivers(message.type()).forEach(c -> c.accept(message));
            }});
            ((JChannelReceiver) channel.getReceiver()).addViewAcceptor(view -> {
                LOG.info("New view: {}, size: {}", view, view.getMembers().size());
                List<Address> members = view.getMembers().stream()
                        .map(a -> new Tuple<>(a, JChannelHelper.physicalAddress(channel, config, a)))
                        .filter(t -> t.getVal2() != null)
                        .peek(t -> addressMap.put(t.getVal2(), t.getVal1()))
                        .map(Tuple::getVal2).collect(Collectors.toList());
                currentView = new ClusterView(view.getViewId().getId(), members);
                LOG.info("New view: {}", currentView);
                clusterEventsListener.accept(ClusterEvent.MEMBERSHIP_CHANGED);
            });
            channel.addChannelListener(new ChannelListener() {
                @Override
                public void channelConnected(Channel channel) {
                    clusterEventsListener.accept(ClusterEvent.CONNECTED);
                }

                @Override
                public void channelDisconnected(Channel channel) {
                    clusterEventsListener.accept(ClusterEvent.DISCONNECTED);
                }

                @Override
                public void channelClosed(Channel channel) {
                    clusterEventsListener.accept(ClusterEvent.CLOSED);
                }
            });
            channel.connect(JGroupsProvider.CLUSTER_NAME);
        } catch (Exception e) {
            throw Exceptions.runtime(e);
        } finally {
            isConnected = true;
        }
    }

    @Override
    public void disconnect() {
        channel.close();
    }

    @Override
    public boolean isConnected() {
        return channel.isConnected();
    }

    @Override
    public ClusterConnector gateway() {
        return connector;
    }

    @Override
    public void marshallWith(Marshaller marshaller) {
        Preconditions.checkNotNull(marshaller, "Marshaller should be non-null.");
        this.marshaller = marshaller;
    }

    @Override
    public void listenEvents(Consumer<ClusterEvent> consumer) {
        this.clusterEventsListener = consumer;
    }

    @Override
    public ClusterView view() {
        return currentView;
    }

    @SuppressWarnings("all")
    public class JGroupsConnector implements ClusterConnector {

        @Override
        public void send(List<Address> dest, Message message) {
            send(dest, message, Collections.emptySet());
        }

        @Override
        public void send(List<Address> dest, Message message, Set<Flag> flags) {
            NettyBasedBuffer buffer = new NettyBasedBuffer();
            marshaller.marshall(buffer, message);
            final byte[] data = buffer.readBytes(buffer.length());

            dest.stream().filter(d -> addressMap.containsKey(d)).map(d -> addressMap.get(d)).forEach(a -> {
                org.jgroups.Message msg = new org.jgroups.Message(a, null, data);
                if (flags.contains(Flag.OUT_OF_BOUND))
                    msg.setFlag(org.jgroups.Message.Flag.OOB);
                if (!flags.contains(Flag.RSVP)) {
                    msg.setFlag(org.jgroups.Message.Flag.NO_RELIABILITY);
                } else if (channel.getProtocolStack().findProtocol(RSVP.class) != null) {
                    msg.setFlag(org.jgroups.Message.Flag.RSVP);
                }
                msg.setTransientFlag(org.jgroups.Message.TransientFlag.DONT_LOOPBACK);
                msg.putHeader(ClusterMessageHeader.ID, new ClusterMessageHeader());

                try {
                    channel.send(msg);
                } catch (Exception e) {
                    throw Exceptions.runtime(e);
                }
            });
        }

        @Override
        public <T extends Message> void receive(int type, Consumer<T> consumer) {
            receivers(type).add((Consumer<Message>) consumer);
        }

        @Override
        public <T extends Message> void receive(int type, Predicate<T> filter, Consumer<T> consumer) {
            receivers(type).add((Message m) -> {
                if (filter.test((T) m)) consumer.accept((T) m);
            });
        }

        @Override
        public <T extends Message> void unsubscribe(int type, Consumer<T> consumer) {
            receivers(type).remove(consumer);
        }
    }

    protected List<Consumer<Message>> receivers(int type) {
        return receivers.computeIfAbsent(type, a -> new CopyOnWriteArrayList<>());
    }

    public JGroupsCluster(RevenoClusterConfiguration config, JChannel channel) {
        this.config = config;
        this.channel = channel;
    }

    protected volatile ClusterView currentView = DEFAULT_VIEW;
    protected volatile boolean isConnected = false;

    protected RevenoClusterConfiguration config;
    protected Consumer<ClusterEvent> clusterEventsListener = (e) -> {};
    protected JGroupsConnector connector = new JGroupsConnector();
    protected Marshaller marshaller = new JsonMarshaller();
    protected Int2ObjectMap<List<Consumer<Message>>> receivers = new Int2ObjectOpenHashMap<>();
    protected Map<InetAddress, org.jgroups.Address> addressMap = new HashMap<>();

    protected JChannel channel;

    protected static final Logger LOG = LoggerFactory.getLogger(JGroupsCluster.class);

    public static class ClusterMessageHeader extends Header {
        public static final short ID = 0x1abc;

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
}
