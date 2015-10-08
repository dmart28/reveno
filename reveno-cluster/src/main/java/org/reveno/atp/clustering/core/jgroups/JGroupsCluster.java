package org.reveno.atp.clustering.core.jgroups;

import org.jgroups.*;
import org.reveno.atp.clustering.api.*;
import org.reveno.atp.clustering.api.Address;
import org.reveno.atp.clustering.api.message.Marshaller;
import org.reveno.atp.clustering.api.message.Message;
import org.reveno.atp.clustering.core.RevenoClusterConfiguration;
import org.reveno.atp.clustering.core.marshallers.JsonMarshaller;
import org.reveno.atp.utils.Exceptions;
import org.reveno.atp.utils.Preconditions;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;

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

                }
                @Override
                public void viewAccepted(View view) {

                }
            });
            channel.addChannelListener(new ChannelListener() {
                @Override
                public void channelConnected(Channel channel) {

                }
                @Override
                public void channelDisconnected(Channel channel) {

                }
                @Override
                public void channelClosed(Channel channel) {

                }
            });

            channel.connect(CLUSTER_NAME);
        } catch (Exception e) {
            throw Exceptions.runtime(e);
        }
    }

    @Override
    public void disconnect() {

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

    public class JGroupsConnector implements ClusterConnector {

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

    protected JChannel channel;
}
