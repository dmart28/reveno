package org.reveno.atp.clustering.core.jgroups;

import org.jgroups.Address;
import org.jgroups.Header;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.protocols.RSVP;
import org.reveno.atp.clustering.api.ClusterBuffer;
import org.reveno.atp.clustering.api.IOMode;
import org.reveno.atp.clustering.core.RevenoClusterConfiguration;
import org.reveno.atp.clustering.util.Tuple;
import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.channel.NettyBasedBuffer;
import org.reveno.atp.utils.Exceptions;

import java.io.DataInput;
import java.io.DataOutput;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * JGroups implementation of {@link ClusterBuffer}. It is intended to be used mainly
 * for test and debug purposes, as it's not very efficient approach in low-latency world.
 */
public class JGroupsBuffer implements ClusterBuffer {

    @Override
    public void connect() {
        if (isConnected) return;
        if (ClassConfigurator.get(ClusterBufferHeader.ID) == null)
            ClassConfigurator.add(ClusterBufferHeader.ID, ClusterBufferHeader.class);

        try {
            ((JChannelReceiver) channel.getReceiver()).addReceiver(msg -> { if (msg.getHeader(ClusterBufferHeader.ID) != null) {
                if (!isLocked) {
                    receiveBuffer.writeBytes(msg.getBuffer());
                    messageListener.accept(JGroupsBuffer.this);
                    receiveBuffer.clear();
                }
            }});
            ((JChannelReceiver) channel.getReceiver()).addViewAcceptor(view -> {
                addresses = view.getMembers().stream()
                        .map(a -> new Tuple<>(a, JChannelHelper.physicalAddress(channel, config, a)))
                        .filter(t -> t.getVal2() != null)
                        .map(t -> new AddressPair(t.getVal1(), t.getVal2().getAddressType()))
                        .sorted((a, b) -> {
                            if (a.mode == IOMode.ASYNC) return 1; else return -1;
                        })
                        .collect(Collectors.toList());
            });
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
    public void messageNotifier(Consumer<ClusterBuffer> listener) {
        this.messageListener = listener;
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
        receiveBuffer.clear();
    }

    @Override
    public void prepare() {
    }

    @Override
    public boolean replicate() {
        byte[] data = sendBuffer.readBytes(sendBuffer.length());
        try {
            addresses.forEach(p -> {
                org.jgroups.Message msg = new org.jgroups.Message(p.address, null, data);
                msg.setTransientFlag(Message.TransientFlag.DONT_LOOPBACK);
                msg.putHeader(ClusterBufferHeader.ID, new ClusterBufferHeader());
                if (p.mode == IOMode.ASYNC) {
                    msg.setFlag(Message.Flag.NO_RELIABILITY);
                    try {
                        channel.send(msg);
                    } catch (Exception e) {
                        // ignore as it's async
                    }
                } else {
                    if (channel.getProtocolStack().findProtocol(RSVP.class) != null) {
                        msg.setFlag(org.jgroups.Message.Flag.RSVP);
                    }
                    try {
                        channel.send(msg);
                    } catch (Exception e) {
                        throw Exceptions.runtime(e);
                    }
                }
            });
        } catch (Exception e) {
            return false;
        } finally {
            sendBuffer.clear();
        }
        return true;
    }

    @Override
    public int readerPosition() {
        return receiveBuffer.readerPosition();
    }

    @Override
    public int writerPosition() {
        return sendBuffer.writerPosition();
    }

    @Override
    public int limit() {
        return receiveBuffer.limit();
    }

    @Override
    public long capacity() {
        return sendBuffer.capacity();
    }

    @Override
    public int length() {
        return sendBuffer.length();
    }

    @Override
    public int remaining() {
        return receiveBuffer.remaining();
    }

    @Override
    public void clear() {
    }

    @Override
    public void release() {
    }

    @Override
    public boolean isAvailable() {
        return receiveBuffer.isAvailable();
    }

    @Override
    public void setReaderPosition(int position) {
        receiveBuffer.setReaderPosition(position);
    }

    @Override
    public void setWriterPosition(int position) {
        sendBuffer.setWriterPosition(position);
    }

    @Override
    public void writeByte(byte b) {
        sendBuffer.writeByte(b);
    }

    @Override
    public void writeBytes(byte[] bytes) {
        sendBuffer.writeBytes(bytes);
    }

    @Override
    public void writeBytes(byte[] buffer, int offset, int count) {
        sendBuffer.writeBytes(buffer, offset, count);
    }

    @Override
    public void writeLong(long value) {
        sendBuffer.writeLong(value);
    }

    @Override
    public void writeInt(int value) {
        sendBuffer.writeInt(value);
    }

    @Override
    public void writeShort(short s) {
        sendBuffer.writeShort(s);
    }

    @Override
    public void writeFromBuffer(ByteBuffer buffer) {
        sendBuffer.writeFromBuffer(buffer);
    }

    @Override
    public ByteBuffer writeToBuffer() {
        return null;
    }

    @Override
    public byte readByte() {
        return receiveBuffer.readByte();
    }

    @Override
    public byte[] readBytes(int length) {
        return receiveBuffer.readBytes(length);
    }

    @Override
    public void readBytes(byte[] data, int offset, int length) {
        receiveBuffer.readBytes(data, offset, length);
    }

    @Override
    public long readLong() {
        return receiveBuffer.readLong();
    }

    @Override
    public int readInt() {
        return receiveBuffer.readInt();
    }

    @Override
    public short readShort() {
        return receiveBuffer.readShort();
    }

    @Override
    public void markReader() {
        receiveBuffer.markReader();
    }

    @Override
    public void markWriter() {
        sendBuffer.markWriter();
    }

    @Override
    public void resetReader() {
        receiveBuffer.markReader();
    }

    @Override
    public void resetWriter() {
        sendBuffer.resetWriter();
    }

    @Override
    public void limitNext(int count) {
    }

    @Override
    public void resetNextLimit() {
    }

    /**
     * Might be called with N depth
     */
    @Override
    public void markSize() {
        sizeMarks.add(sendBuffer.writerPosition());
        sendBuffer.writeInt(0);
    }

    @Override
    public int sizeMarkPosition() {
        return sizeMarks.peek();
    }

    @Override
    public void writeSize() {
        int pos = sendBuffer.writerPosition();
        sendBuffer.setWriterPosition(sizeMarks.poll());
        sendBuffer.writeInt(pos - sendBuffer.writerPosition() - 4);
        sendBuffer.setWriterPosition(pos);
    }

    public JGroupsBuffer(RevenoClusterConfiguration config, JChannel channel) {
        this.channel = channel;
        this.config = config;
    }

    protected JChannel channel;
    protected RevenoClusterConfiguration config;

    protected volatile boolean isConnected = false;
    protected volatile boolean isLocked = false;
    protected volatile List<AddressPair> addresses = new ArrayList<>();
    protected Consumer<ClusterBuffer> messageListener = (b) -> {};
    protected Buffer sendBuffer = new NettyBasedBuffer();
    protected Buffer receiveBuffer = new NettyBasedBuffer();
    protected Queue<Integer> sizeMarks = new LinkedTransferQueue<>();

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
