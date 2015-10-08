package org.reveno.atp.clustering.core.jgroups;

import org.reveno.atp.clustering.api.ClusterBuffer;
import org.reveno.atp.core.api.channel.Buffer;

import java.nio.ByteBuffer;

/**
 * JGroups implementation of {@link ClusterBuffer}. It is intended to be used mainly
 * for test and debug purposes, as it's not very efficient approach in low-latency world.
 */
public class JGroupsBuffer implements ClusterBuffer {

    @Override
    public void connect() {

    }

    @Override
    public void disconnect() {

    }

    @Override
    public void messageNotifier(Runnable listener) {

    }

    @Override
    public void lockIncoming() {

    }

    @Override
    public void unlockIncoming() {

    }

    @Override
    public void erase() {

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
}
