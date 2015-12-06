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

package org.reveno.atp.clustering.api;

import java.util.List;

public interface ClusterConfiguration {

    /**
     * The network address of the current node and unique Node ID.
     * Depending on the concrete {@link Cluster} and {@link ClusterBuffer} implementation,
     * used in current Reveno instance, this host and port will be used for tcp binding,
     * and Node ID for unique representation of the current node with multicast failover.
     *
     * @param host of the current node in cluster
     * @param port of the current node in cluster
     * @param nodeId unqiue node identificator in among cluster
     */
    void currentNodeAddress(String host, String port, String nodeId);

    /**
     * The network address of the current node and unique Node ID.
     * Depending on the concrete {@link org.reveno.atp.clustering.core.buffer.ClusterProvider} implementation,
     * used in current Reveno instance, this host and port will be used for tcp binding,
     * and Node ID for unique representation of the current node with multicast failover.
     *
     * @param host of the current node in cluster
     * @param port of the current node in cluster
     * @param nodeId unqiue node identificator in among cluster
     * @param mode of IO between other nodes
     */
    void currentNodeAddress(String host, String port, String nodeId, IOMode mode);

    /**
     * Abstract Address which identifies current node in cluster. It should be
     * provided in the format, which currently used {@link org.reveno.atp.clustering.core.buffer.ClusterProvider}
     * can understand it.
     *
     * @param nodeAddress of current node in Reveno cluster
     */
    void currentNodeAddress(Address nodeAddress);

    /**
     * List of other parties network addresses.
     * @param addresses
     */
    void nodesInetAddresses(List<InetAddress> addresses);

    /**
     * List of other parties addresses.
     * @param nodeAddresses
     */
    void nodesAddresses(List<Address> nodeAddresses);

    /**
     * Denotes the style of network communication which will be used between
     * all nodes in cluster to transfer commands from Master to Slaves. Currently,
     * it supports two possible values: UNICAST and MULTICAST. You should also note, that
     * with any value used here, all communications between nodes are reliable, means the loss or
     * corruption of data is always processed according to the situation, guaranteeing ordering,
     * data integrity, and etc.
     *
     * UNICAST is a one-to one connection between all hosts in cluster. It uses TCP NIO2 under
     * the hood. It is most reliable option since TCP by itself provide ordering and ack capabilities,
     * but in some environments it might scale not so well.
     *
     * MULTICAST works like broadcast, with differences like subscription basis, etc. It uses UDP protocol,
     * which means that no connections to other parties will be opened, and the packet will be sent only once,
     * routed to many. With good network configuration, it scales much better than TCP. But be aware, that usually
     * multicast connections are not possible in internet environment.
     *
     * Read more about all options on http://reveno.org
     *
     * @param transport type to be used for commands transfer.
     */
    void commandsXmitTransport(CommandsXmitTransport transport);

    /**
     * Multicast configurations, which will be used in case of {@link #commandsXmitTransport(CommandsXmitTransport)}
     * was set to MULTICAST.
     * @return multicast configuration
     */
    MulticastConfiguration multicast();

    /**
     * Unicast configrations, which will be used in cast of {@link #commandsXmitTransport(CommandsXmitTransport)}
     * was set to UNICAST.
     * @return unicast configuration
     */
    UnicastConfiguration unicast();

    /**
     * Timeouts which are used during Leadership Election process, which is started usually by membership
     * changes, or some unexpected critical situations.
     * @return timeouts configuration
     */
    TimeoutsConfiguration electionTimeouts();

    /**
     * Data Synchronization configurations. Data Synchronization is a process, which is issued at some stage
     * of Leadership Election process. First, nodes sends to each other their last transaction IDs processed.
     * After that, somes might discover that their internal state is not the last in cluster. In that case,
     * they ask the node with the most actual state to share last transactions / snapshot, efficiently syncing
     * with it. After all nodes share the same state, they can continue to operate.
     *
     * @return sync configuration
     */
    SyncConfiguration dataSync();

    void authToken(String authToken);

    /**
     * Priority in cluster, which allows to effectively foresee next elected Masters in case
     * of failure of previous ones. The lower value, the more possibility that this node will be
     * elected as Master in next election round.
     *
     * @param priority of the node
     */
    void priorityInCluster(int priority);


    interface TimeoutsConfiguration {
        void voteTimeoutNanos(long timeout);

        void syncTimeoutNanos(long timeout);

        void ackTimeoutNanos(long timeout);

        void barrierTimeoutNanos(long timeout);

        void syncBarrierTimeoutNanos(long timeout);
    }

    interface SyncConfiguration {
        /**
         * A mode in which two nodes will perform synchronization during Leadership Election. First option means that
         * all nodes makes snapshots in advance and then just send it by request from other nodes.
         * Second option is when the node sends required Journal files to other nodes, which contains events with
         * transaction ID more or equals to current node last transaction ID.
         *
         * First option is prefered, but is not recommended for big models, because of size of snapshot
         * and costs spent to make it for each Leadership Election process.
         *
         * Second option is generally not recommended if there is a big amount of Journal files or rolling
         * is performed very rarely, means big files will be always transmitted.
         *
         * Default value is SyncMode.SNAPSHOT
         * @param mode
         */
        void mode(SyncMode mode);

        /**
         * Thread pool size of Sync Server, which handles synchronization requests and sends appropriate
         * data.
         * @param threads to be used
         */
        void threadPoolSize(int threads);

        /**
         * Number of retries to transmit state data until it fails.
         * @param count
         */
        void retries(int count);

        /**
         * Port on which Sync Server will be listening.
         * @param port
         */
        void port(int port);

        /**
         * The synchronization process lasts during global Leadership Election process, which means
         * no node during it can operate on production. Sometimes, it is obvious that Master
         * candidate has the latest state, and can start to operate not awaiting other Slaves to catch up with
         * its changes.
         * In that case, if waitAllNodesSync is set to true, it will start to handle Commands normally
         * during "sync" stage of Leadership Election process.
         *
         * Warning!!! You should consider using that flag *very* carefully, as it means that all failover data
         * from Master will not be handled by Slaves as usual, but just buffered in memory, until they finish syncing.
         * And if synchronization process takes too long, some data might be even lost. Use it primarily if
         * you want Master to start operate as soon as possible (millisends matters) and you know that sync process
         * either not happens at all or will be very short.
         *
         * @param wait
         */
        void waitAllNodesSync(boolean wait);
    }

    interface MulticastConfiguration {
        /**
         * Multicast host.
         * @param host
         */
        void host(String host);

        /**
         * Multicast port.
         * @param port
         */
        void port(int port);

        /**
         * Network interface to be used by Multicast socket. Default is lo.
         * @param netInterface
         */
        void netInterface(String netInterface);

        /**
         * Socket receive buffer size. Default is 1MB.
         * @param size
         */
        void receiveBufferSize(int size);

        /**
         * Socket send buffer size. Default is 1MB.
         * @param size
         */
        void sendBufferSize(int size);

        /**
         * Limits the amount of packets per seconds that might be sent to multicast socket
         * directly. You should configure that according to your network capabilities.
         * Please note, that it is the same as "Commands per second" in
         * Reveno terms only if {@link #preferBatchingToLatency(boolean)} is set to {@code false}.
         *
         * Default value is 3k.
         * @param pps
         */
        void packetsPerSecond(int pps);

        /**
         * Microseconds during which busy spin loop awaiting new packets will be performed.
         * 0 - blocking mode.
         *
         * Default value is 0.
         * @param micros
         */
        void spinLoopMicros(int micros);

        /**
         * Microseconds during which LockSupport.parkNanos() will be called in busy spin loop.
         * If {@link #spinLoopMicros(int)} is set to 0, this setting makes no sense.
         * @param micros
         */
        void threadParkMicros(int micros);

        /**
         * The number of packets which will be persistently stored in offheap memory for
         * NAK retransmition purpose. For example, if pps is 3,000, and packet history is 9,000 then
         * packets for the last 3 seconds might be retransmitted again on request.
         * @param packets
         */
        void retransmitPacketsHistory(int packets);

        /**
         * For lowest latency choose a packet size slightly lower than netork MTU.
         * For high throughput choose larger packet sizes (up to 65k). Downside of large packet sizes is,
         * that a packet gap has worse effects (because e.g. 64k need to be retransmitted instead of just
         * 1k). As history and receive buffers reserve N*full packet size number of bytes, large packets
         * also increase required memory to hold buffers. Its good practice to choose multiples of MTU
         * for packet sizes, though its not that significant. Usual values are 1.5k, 3k, 8k, 16k . 64k
         * are also a possible setting (but large buffers). Recommendation is 4k to 8k. For low latency
         * requirements set small mtu sizes on your network adapter and a packet size fitting into a
         * single mtu size.
         *
         * Default is 1k.
         * @param size
         */
        void packetSize(int size);

        /**
         * Setting that denotes whether to flush every on every Command processed or to buffer it
         * until max packet size is reached. Set {@code false} for real low latency and stronger failover
         * guarantees.
         *
         * Default value is true.
         * @param batching
         */
        void preferBatchingToLatency(boolean batching);

        /**
         * Milticast transport TTL (Time to live).
         *
         * Read more about it here: http://www.tldp.org/HOWTO/Multicast-HOWTO-2.html
         *
         * @param ttl
         */
        void ttl(int ttl);

        /**
         * An amount of retries to send in case of PPS limit reached, send buffers full,
         * or initialization is not yet fully happend.
         *
         * Default value is 15.
         * @param retries
         */
        void sendRetries(int retries);
    }

    interface UnicastConfiguration {
        /**
         * Socket receive buffer size. Default 1MB.
         * @param size
         */
        void receiveBufferSize(int size);

        /**
         * Socket send buffer size. Default 1MB.
         * @param size
         */
        void sendBufferSize(int size);

        /**
         * Timeout for ping heartbeat and discovery requests.
         * Defaults to 5 seconds.
         * @param pingTimeout
         */
        void pingTimeoutMillis(int pingTimeout);

        /**
         * Minimum number of threads to be used for messages handling.
         * Default is 1.
         * @param threads
         */
        void minReceiveThreads(int threads);

        /**
         * Maximum number of threads to be used for messages handling.
         * Default is 1.
         * @param threads
         */
        void maxReceiveThreads(int threads);

        /**
         * The size of the queue which will be used when all handling threads
         * are busy. 0 disables it.
         *
         * Default is 0.
         * @param size
         */
        void receiveQueueMaxSize(int size);

        /**
         * Max number of messages a read will try to read from the socket.
         * Setting this to a higher value will increase speed when receiving a lot of messages.
         * However, when the receive message rate is small, then every read will create an array of
         * max_read_batch_size messages.
         *
         * Default is 10.
         * @param size
         */
        void maxReadBatchMessages(int size);

        /**
         * Interval (in milliseconds) at which messages in the send windows are resent.
         *
         * Default is 1 sec.
         * @param millis
         */
        void retransmitIntervalMillis(int millis);

        /**
         * Max number of milliseconds we try to retransmit a message to any given member.
         * After that, the connection is removed. 0 disables this
         *
         * Default is 3 sec.
         * @param millis
         */
        void maxRetransmitTimeMillis(int millis);
    }

    enum CommandsXmitTransport {
        UNICAST, MULTICAST
    }
}
