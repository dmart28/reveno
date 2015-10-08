package org.reveno.atp.clustering.core.jgroups;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jgroups.*;
import org.reveno.atp.clustering.api.*;
import org.reveno.atp.clustering.api.Address;
import org.reveno.atp.clustering.api.message.Marshaller;
import org.reveno.atp.clustering.api.message.Message;
import org.reveno.atp.clustering.core.RevenoClusterConfiguration;
import org.reveno.atp.clustering.core.marshallers.JsonMarshaller;
import org.reveno.atp.core.channel.NettyBasedBuffer;
import org.reveno.atp.utils.Exceptions;
import org.reveno.atp.utils.Preconditions;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class JGroupsCluster implements Cluster {
    protected static final String CLUSTER_NAME = "rvno_jg";
    protected static final ClusterView DEFAULT_VIEW = new ClusterView(0, Collections.emptyList());

    @Override
    public void connect() {
        System.setProperty("jgroups.tcp.bind_addr", ((InetAddress) config.currentNodeAddress()).getHost());
        System.setProperty("jgroups.tcp.bind_port", Integer.toString(((InetAddress) config.currentNodeAddress()).getPort()));
        System.setProperty("jgroups.tcpping.initial_hosts", makeInitialHostsString(config.clusterNodeAddresses()));
        System.setProperty("jgroups.rsvp.timeout", Long.toString(config.revenoTimeouts().ackTimeout()));
        System.setProperty("jgroups.auth.token", Optional.ofNullable(config.authToken()).orElse(""));

        try {
            if (configFilePath.startsWith("classpath:/")) {
                channel = new JChannel(getClass().getClassLoader().getResourceAsStream(configFilePath.replace("classpath:/", "")));
            } else {
                channel = new JChannel(new File(configFilePath));
            }

            channel.setReceiver(new ReceiverAdapter() {
                @Override
                public void receive(org.jgroups.Message msg) {
                    // no zero copy as not critical transaction processing part
                    NettyBasedBuffer buffer = new NettyBasedBuffer(msg.getLength(), msg.getLength(), false);
                    buffer.writeBytes(msg.getBuffer());
                    Message message = marshaller.unmarshall(buffer);
                    message.address(physicalAddress(msg.getSrc()));

                    receivers(message.type()).forEach(c -> c.accept(message));
                }
                @Override
                public void viewAccepted(View view) {
                    currentView = new ClusterView(view.getViewId().getId(), view.getMembers().stream()
                            .map(JGroupsCluster.this::physicalAddress).collect(Collectors.toList()));
                    clusterEventsListener.accept(ClusterEvent.MEMBERSHIP_CHANGED);
                }
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

            channel.connect(CLUSTER_NAME);
        } catch (Exception e) {
            throw Exceptions.runtime(e);
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

    protected String makeInitialHostsString(List<Address> addresses) {
        StringBuilder sb = new StringBuilder();
        addresses.stream().map(a -> (InetAddress)a).forEach(a -> sb.append(a.getHost())
                .append("[").append(a.getPort()).append("]").append(","));
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    @SuppressWarnings("all")
    public class JGroupsConnector implements ClusterConnector {

        @Override
        public CompletableFuture<Boolean> send(List<Address> dest, Message message) {
            return send(dest, message, Collections.emptySet());
        }

        @Override
        public CompletableFuture<Boolean> send(List<Address> dest, Message message, Set<Flag> flags) {
            NettyBasedBuffer buffer = new NettyBasedBuffer();
            marshaller.marshall(buffer, message);
            final byte[] data = buffer.readBytes(buffer.length());

            physicalAddresses(dest.stream().map(a -> (InetAddress)a).collect(Collectors.toList())).forEach(a -> {
                org.jgroups.Message msg = new org.jgroups.Message(a, null, data);
                if (flags.contains(Flag.OUT_OF_BOUND))
                    msg.setFlag(org.jgroups.Message.Flag.OOB);
                if (flags.contains(Flag.RSVP))
                    msg.setFlag(org.jgroups.Message.Flag.RSVP);

                try {
                    channel.send(msg);
                } catch (Exception e) {
                    throw Exceptions.runtime(e);
                }
            });
            return null;
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
        return receivers.computeIfAbsent(type, a -> new ArrayList<>());
    }

    protected InetAddress physicalAddress(org.jgroups.Address address) {
        PhysicalAddress physicalAddress = (PhysicalAddress)
                channel.down(
                        new Event(Event.GET_PHYSICAL_ADDRESS, address)
                );
        String[] parts = physicalAddress.toString().split(":");
        return config.clusterNodeAddresses().stream().map(a -> (InetAddress) a).filter(a -> a.getHost()
                .equals(parts[0]) && a.getPort() == Integer.parseInt(parts[1])).findFirst().get();
    }

    protected List<org.jgroups.Address> physicalAddresses(List<InetAddress> addresses) {
        return channel.getView().getMembers().stream().map(a -> (PhysicalAddress)
                channel.down(new Event(Event.GET_PHYSICAL_ADDRESS, a)))
                .filter(a -> addresses.stream().anyMatch(as -> as.toString().equals(a.toString())))
                .collect(Collectors.toList());
    }

    public JGroupsCluster(RevenoClusterConfiguration config, String configFilePath) {
        this.config = config;
        this.configFilePath = configFilePath;
    }

    protected volatile ClusterView currentView = DEFAULT_VIEW;

    protected RevenoClusterConfiguration config;
    protected String configFilePath;
    protected Consumer<ClusterEvent> clusterEventsListener = (e) -> {};
    protected JGroupsConnector connector = new JGroupsConnector();
    protected Marshaller marshaller = new JsonMarshaller();
    protected Int2ObjectMap<List<Consumer<Message>>> receivers = new Int2ObjectOpenHashMap<>();

    protected JChannel channel;
}
