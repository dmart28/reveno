package org.reveno.atp.clustering.core.jgroups;

import org.jgroups.Header;
import org.jgroups.JChannel;
import org.jgroups.conf.ClassConfigurator;
import org.reveno.atp.clustering.api.ClusterBuffer;
import org.reveno.atp.clustering.core.RevenoClusterConfiguration;
import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.channel.NettyBasedBuffer;
import org.reveno.atp.utils.Exceptions;

import java.io.DataInput;
import java.io.DataOutput;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * JGroups implementation of {@link ClusterBuffer}. It is intended to be used mainly
 * for test and debug purposes, as it's not very efficient approach in low-latency world.
 */
public class JGroupsBuffer implements ClusterBuffer {

    @Override
    public void connect() {
        if (ClassConfigurator.get(ClusterBufferHeader.ID) == null)
            ClassConfigurator.add(ClusterBufferHeader.ID, ClusterBufferHeader.class);

        try {
            ((JChannelReceiver) channel.getReceiver()).addReceiver(msg -> { if (msg.getHeader(ClusterBufferHeader.ID) != null) {
                if (!isLocked) {
                    receiveBuffer.writeBytes(msg.getBuffer());
                    messageListener.run();
                    receiveBuffer.clear();
                }
            }});
        } catch (Exception e) {
            throw Exceptions.runtime(e);
        }
    }

    @Override
    public void disconnect() {
        channel.disconnect();
    }

    @Override
    public void messageNotifier(Runnable listener) {
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
    public boolean replicate() {
        return false;
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

    protected volatile boolean isLocked = false;
    protected Runnable messageListener = () -> {};
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
}
