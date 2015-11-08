package org.reveno.atp.clustering.core.components;

import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.channel.NettyBasedBuffer;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.LinkedTransferQueue;

public abstract class AbstractClusterBuffer {

    public int readerPosition() {
        throw new UnsupportedOperationException();
    }

    public int limit() {
        throw new UnsupportedOperationException();
    }

    public int remaining() {
        throw new UnsupportedOperationException();
    }

    public void clear() {
    }

    public void release() {
    }
    
    public boolean isAvailable() {
        throw new UnsupportedOperationException();
    }

    public void setReaderPosition(int position) {
        throw new UnsupportedOperationException();
    }

    public ByteBuffer writeToBuffer() {
        return null;
    }

    public byte readByte() {
        throw new UnsupportedOperationException();
    }

    public byte[] readBytes(int length) {
        throw new UnsupportedOperationException();
    }
    
    public void readBytes(byte[] data, int offset, int length) {
        throw new UnsupportedOperationException();
    }
    
    public long readLong() {
        throw new UnsupportedOperationException();
    }

    public int readInt() {
        throw new UnsupportedOperationException();
    }

    public short readShort() {
        throw new UnsupportedOperationException();
    }

    
    public void markReader() {
        throw new UnsupportedOperationException();
    }

    public void resetReader() {
        throw new UnsupportedOperationException();
    }

    public void limitNext(int count) {
    }

    public void resetNextLimit() {
    }

    public int writerPosition() {
        return sendBuffer.writerPosition();
    }

    public long capacity() {
        return sendBuffer.capacity();
    }

    public int length() {
        return sendBuffer.length();
    }

    public void setWriterPosition(int position) {
        sendBuffer.setWriterPosition(position);
    }

    public void writeByte(byte b) {
        sendBuffer.writeByte(b);
    }

    public void writeBytes(byte[] bytes) {
        sendBuffer.writeBytes(bytes);
    }

    public void writeBytes(byte[] buffer, int offset, int count) {
        sendBuffer.writeBytes(buffer, offset, count);
    }

    public void writeLong(long value) {
        sendBuffer.writeLong(value);
    }

    public void writeInt(int value) {
        sendBuffer.writeInt(value);
    }

    public void writeShort(short s) {
        sendBuffer.writeShort(s);
    }

    public void writeFromBuffer(ByteBuffer buffer) {
        sendBuffer.writeFromBuffer(buffer);
    }

    public void resetWriter() {
        sendBuffer.resetWriter();
    }

    public void markWriter() {
        sendBuffer.markWriter();
    }

    /**
     * Might be called with N depth
     */
    public void markSize() {
        sizeMarks.add(sendBuffer.writerPosition());
        sendBuffer.writeInt(0);
    }

    public int sizeMarkPosition() {
        return sizeMarks.peek();
    }

    public void writeSize() {
        int pos = sendBuffer.writerPosition();
        sendBuffer.setWriterPosition(sizeMarks.poll());
        sendBuffer.writeInt(pos - sendBuffer.writerPosition() - 4);
        sendBuffer.setWriterPosition(pos);
    }

    protected Buffer sendBuffer = new NettyBasedBuffer();
    protected Queue<Integer> sizeMarks = new LinkedTransferQueue<>();

}
