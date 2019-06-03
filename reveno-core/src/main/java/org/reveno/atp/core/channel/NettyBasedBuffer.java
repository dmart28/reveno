package org.reveno.atp.core.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import org.reveno.atp.core.api.channel.Buffer;

import java.nio.ByteBuffer;

import static org.reveno.atp.utils.MeasureUtils.kb;

public class NettyBasedBuffer implements Buffer {
    private final ByteBuf buffer;

    public NettyBasedBuffer() {
        this(kb(3), true);
    }

    public NettyBasedBuffer(boolean direct) {
        this(kb(1), direct);
    }

    public NettyBasedBuffer(int length, boolean direct) {
        this.buffer = direct ? PooledByteBufAllocator.DEFAULT.directBuffer(length) : PooledByteBufAllocator.DEFAULT.buffer(length);
    }

    public NettyBasedBuffer(int length, int maxLength, boolean direct) {
        this.buffer = direct ? PooledByteBufAllocator.DEFAULT.directBuffer(length, maxLength) : PooledByteBufAllocator.DEFAULT.buffer(length, maxLength);
    }

    @Override
    public int readerPosition() {
        return buffer.readerIndex();
    }

    @Override
    public int writerPosition() {
        return buffer.writerIndex();
    }

    @Override
    public int limit() {
        return buffer.writerIndex();
    }

    @Override
    public long capacity() {
        return buffer.capacity();
    }

    @Override
    public int length() {
        return buffer.writerIndex();
    }

    @Override
    public boolean isAvailable() {
        return buffer.writerIndex() - buffer.readerIndex() > 0;
    }

    @Override
    public void setReaderPosition(int position) {
        buffer.readerIndex(position);
    }

    @Override
    public void setWriterPosition(int position) {
        buffer.writerIndex(position);
    }

    @Override
    public void writeByte(byte b) {
        buffer.writeByte(b);
    }

    @Override
    public int remaining() {
        return buffer.readableBytes();
    }

    @Override
    public void clear() {
        buffer.clear();
    }

    @Override
    public void release() {
        if (buffer.refCnt() != 0)
            buffer.release();
    }

    @Override
    public void writeBytes(byte[] bytes) {
        buffer.writeBytes(bytes);
    }

    @Override
    public void writeBytes(byte[] bytes, int offset, int count) {
        buffer.writeBytes(bytes, offset, count);
    }

    @Override
    public void writeLong(long value) {
        buffer.writeLong(value);
    }

    @Override
    public void writeInt(int value) {
        buffer.writeInt(value);
    }

    @Override
    public void writeShort(short s) {
        buffer.writeShort(s);
    }

    @Override
    public void writeFromBuffer(ByteBuffer bb) {
        buffer.writeBytes(bb);
    }

    @Override
    public ByteBuffer writeToBuffer() {
        return buffer.nioBuffer();
    }

    @Override
    public byte readByte() {
        return buffer.readByte();
    }

    @Override
    public byte[] readBytes(int length) {
        byte[] data = new byte[length];
        buffer.readBytes(data, 0, data.length > buffer.readableBytes() ? buffer.readableBytes() : data.length);
        return data;
    }

    @Override
    public void readBytes(byte[] data, int offset, int length) {
        buffer.readBytes(data, offset, length);
    }

    @Override
    public long readLong() {
        return buffer.readLong();
    }

    @Override
    public int readInt() {
        return buffer.readInt();
    }

    @Override
    public short readShort() {
        return buffer.readShort();
    }

    @Override
    public int hashCode() {
        return buffer.hashCode();
    }

    @Override
    public void markReader() {
        buffer.markReaderIndex();
    }

    @Override
    public void markWriter() {
        buffer.markWriterIndex();
    }

    @Override
    public void resetReader() {
        buffer.resetReaderIndex();
    }

    @Override
    public void resetWriter() {
        buffer.resetWriterIndex();
    }

    @Override
    public void limitNext(int count) {
    }

    @Override
    public void resetNextLimit() {
    }

    @Override
    public void markSize() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int sizeMarkPosition() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeSize() {
        throw new UnsupportedOperationException();
    }

}
