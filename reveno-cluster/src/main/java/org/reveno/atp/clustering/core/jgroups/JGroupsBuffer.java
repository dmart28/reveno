package org.reveno.atp.clustering.core.jgroups;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
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
            initDisruptor();

            ((JChannelReceiver) channel.getReceiver()).addReceiver(msg -> { if (msg.getHeader(ClusterBufferHeader.ID) != null) {
                if (!isLocked) {
                    disruptor.publishEvent((e,s) -> e.reset().message = msg);
                }
            }});
        } catch (Exception e) {
            throw Exceptions.runtime(e);
        }
    }

    @Override
    public void disconnect() {
        channel.disconnect();
        executor.shutdown();
    }

    @Override
    public void messageNotifier(Runnable listener) {
        this.messageListener = listener;
    }

    @Override
    public void lockIncoming() {
        isLocked = true;
        CompletableFuture<Void> f = new CompletableFuture<>();
        disruptor.publishEvent((e,s) -> e.reset().future = f);
        try {
            f.get();
        } catch (InterruptedException | ExecutionException e) {
            throw Exceptions.runtime(e);
        }
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
        return 0;
    }

    @Override
    public int writerPosition() {
        return 0;
    }

    @Override
    public int limit() {
        return 0;
    }

    @Override
    public long capacity() {
        return 0;
    }

    @Override
    public int length() {
        return 0;
    }

    @Override
    public int remaining() {
        return 0;
    }

    @Override
    public void clear() {

    }

    @Override
    public void release() {

    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void setReaderPosition(int position) {

    }

    @Override
    public void setWriterPosition(int position) {

    }

    @Override
    public void writeByte(byte b) {

    }

    @Override
    public void writeBytes(byte[] bytes) {

    }

    @Override
    public void writeBytes(byte[] buffer, int offset, int count) {

    }

    @Override
    public void writeLong(long value) {

    }

    @Override
    public void writeInt(int value) {

    }

    @Override
    public void writeShort(short s) {

    }

    @Override
    public void writeFromBuffer(ByteBuffer buffer) {

    }

    @Override
    public ByteBuffer writeToBuffer() {
        return null;
    }

    @Override
    public byte readByte() {
        return 0;
    }

    @Override
    public byte[] readBytes(int length) {
        return new byte[0];
    }

    @Override
    public void readBytes(byte[] data, int offset, int length) {

    }

    @Override
    public long readLong() {
        return 0;
    }

    @Override
    public int readInt() {
        return 0;
    }

    @Override
    public short readShort() {
        return 0;
    }

    @Override
    public void markReader() {

    }

    @Override
    public void markWriter() {

    }

    @Override
    public void resetReader() {

    }

    @Override
    public void resetWriter() {

    }

    @Override
    public void limitNext(int count) {

    }

    @Override
    public void resetNextLimit() {

    }

    @Override
    public void markSize() {

    }

    @Override
    public int sizeMarkPosition() {
        return 0;
    }

    @Override
    public void writeSize() {

    }

    protected void initDisruptor() {
        disruptor = new Disruptor<>(factory, 1024, executor, ProducerType.MULTI, new SleepingWaitStrategy());
        disruptor.handleEventsWith((e,s,b) -> {
            if (e.future == null)
                receiveBuffer.writeBytes(e.message.getBuffer());
            else
                e.future.complete(null);
        }).then((e,s,b) -> { if (e.future == null) messageListener.run(); });
        disruptor.start();
    }

    public JGroupsBuffer(RevenoClusterConfiguration config, JChannel channel) {
        this.channel = channel;
        this.config = config;
    }

    protected JChannel channel;
    protected RevenoClusterConfiguration config;

    protected Disruptor<NewMessageEvent> disruptor;
    protected ExecutorService executor = Executors.newFixedThreadPool(2);

    protected volatile boolean isLocked = false;
    protected Runnable messageListener = () -> {};
    protected Buffer sendBuffer = new NettyBasedBuffer();
    protected Buffer receiveBuffer = new NettyBasedBuffer();
    protected EventFactory<NewMessageEvent> factory = NewMessageEvent::new;

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

    protected static class NewMessageEvent {
        public org.jgroups.Message message;
        public CompletableFuture<Void> future;

        public NewMessageEvent reset() {
            this.future = null;
            this.message = null;
            return this;
        }
    }
}
