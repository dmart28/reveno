package org.reveno.atp.clustering.core.components;

import org.reveno.atp.clustering.api.ClusterView;
import org.reveno.atp.clustering.api.InetAddress;
import org.reveno.atp.clustering.core.RevenoClusterConfiguration;
import org.reveno.atp.clustering.core.api.ClusterExecutor;
import org.reveno.atp.clustering.core.messages.NodeState;
import org.reveno.atp.core.api.channel.Channel;
import org.reveno.atp.core.api.storage.JournalsStorage;

import org.reveno.atp.clustering.core.components.StorageTransferModelSync.TransferContext;
import org.reveno.atp.utils.Exceptions;
import org.reveno.atp.utils.MeasureUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class StorageTransferModelSync implements ClusterExecutor<Boolean, TransferContext> {

    @Override
    public Boolean execute(ClusterView currentView, TransferContext context) {
        String host = ((InetAddress) context.latestNode.address()).getHost();
        int port = context.latestNode.syncPort;
        SocketAddress sad = new InetSocketAddress(host, port);
        JournalsStorage.JournalStore tempStore = storage.nextTempStore();
        JournalsStorage.JournalStore store = storage.nextStore(context.latestNode.transactionId);

        if (receiveStore(context, sad, TRANSACTIONS, storage.channel(tempStore.getTransactionCommitsAddress())) &&
                receiveStore(context, sad, EVENTS, storage.channel(tempStore.getEventsCommitsAddress()))) {
            storage.mergeStores(new JournalsStorage.JournalStore[] { tempStore }, store);
            return true;
        } else {
            storage.deleteStore(tempStore);
            return false;
        }
    }

    protected boolean receiveStore(TransferContext context, SocketAddress sad, byte type, Channel channel) {
        try {
            SocketChannel sc = SocketChannel.open();
            if (!sc.connect(sad)) {
                LOG.error("Can't establish connection to {}", sad);
                return false;
            }
            sc.configureBlocking(true);

            ByteBuffer message = ByteBuffer.allocate(10);
            message.put(config.revenoSync().mode().getType());
            message.put(type);
            message.putLong(context.transactionId);
            sc.write(message);

            ByteBuffer data = ByteBuffer.allocate(MeasureUtils.kb(64));
            int nread = 0;
            while (nread != -1)  {
                try {
                    nread = sc.read(data);
                    channel.write(b -> b.writeFromBuffer(data), true);
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                    throw Exceptions.runtime(e);
                }
                data.rewind();
            }
        } catch (Throwable t) {
            LOG.error("Can't sync with remote node " + sad, t);
            return false;
        } finally {
            channel.close();
        }
        return true;
    }


    public StorageTransferModelSync(RevenoClusterConfiguration config, JournalsStorage storage) {
        this.config = config;
        this.storage = storage;
    }

    protected RevenoClusterConfiguration config;
    protected JournalsStorage storage;

    protected static final Logger LOG = LoggerFactory.getLogger(StorageTransferModelSync.class);
    public static final byte TRANSACTIONS = 1;
    public static final byte EVENTS = 2;

    public static class TransferContext {
        public final long transactionId;
        public final NodeState latestNode;

        public TransferContext(long transactionId, NodeState latestNode) {
            this.transactionId = transactionId;
            this.latestNode = latestNode;
        }
    }

}
