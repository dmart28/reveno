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

package org.reveno.atp.clustering.core.fastcast;

import org.nustaq.offheap.bytez.Bytez;
import org.reveno.atp.clustering.util.Bits;
import org.reveno.atp.core.api.channel.Buffer;

import java.nio.ByteBuffer;

public class BytezBufferWrapper implements Buffer {

    @Override
    public int readerPosition() {
        return pos;
    }

    @Override
    public int writerPosition() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int limit() {
        return length;
    }

    @Override
    public long capacity() {
        return length;
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public int remaining() {
        return length - pos;
    }

    @Override
    public void clear() {
    }

    @Override
    public void release() {
    }

    @Override
    public boolean isAvailable() {
        return remaining() > 0;
    }

    @Override
    public void setReaderPosition(int position) {
        this.pos = position;
    }

    @Override
    public void setWriterPosition(int position) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeByte(byte b) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeBytes(byte[] bytes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeBytes(byte[] buffer, int offset, int count) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeLong(long value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeInt(int value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeShort(short s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeFromBuffer(ByteBuffer buffer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteBuffer writeToBuffer() {
        byte[] bytes = readBytes(length - pos);
        return ByteBuffer.wrap(bytes);
    }

    @Override
    public byte readByte() {
        byte b = bytez.get(off + pos);
        pos++;
        return b;
    }

    @Override
    public byte[] readBytes(int length) {
        byte[] data = new byte[length];
        readBytes(data, 0, length);
        return data;
    }

    @Override
    public void readBytes(byte[] data, int offset, int length) {
        bytez.getArr(off + pos, data, offset, length);
        pos += length;
    }

    @Override
    public long readLong() {
        long l = Bits.readLong(bytez.get(off + pos), bytez.get(off + pos + 1),
                bytez.get(off + pos + 2), bytez.get(off + pos + 3), bytez.get(off + pos + 4),
                bytez.get(off + pos + 5), bytez.get(off + pos + 6), bytez.get(off + pos + 7));
        pos += 8;
        return l;
    }

    @Override
    public int readInt() {
        int i = Bits.readInt(bytez.get(off + pos), bytez.get(off + pos + 1),
                bytez.get(off + pos + 2), bytez.get(off + pos + 3));
        pos += 4;
        return i;
    }

    @Override
    public short readShort() {
        short s = Bits.readShort(bytez.get(off + pos), bytez.get(off + pos + 1));
        pos += 2;
        return s;
    }

    @Override
    public void markReader() {
    }

    @Override
    public void markWriter() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void resetReader() {
    }

    @Override
    public void resetWriter() {
        throw new UnsupportedOperationException();
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

    public Bytez getBytez() {
        return bytez;
    }

    public void setBytez(Bytez bytez, long off, int len) {
        this.bytez = bytez;
        this.off = off;
        this.length = len;
        this.pos = 0;
    }

    protected Bytez bytez;
    protected long off;
    protected int length;

    protected int pos;
}
