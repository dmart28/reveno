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

package org.reveno.atp.core.serialization.protostuff;

import io.protostuff.LinkBuffer;
import org.reveno.atp.core.api.channel.Buffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public class ZeroCopyLinkBuffer extends LinkBuffer {

    protected Buffer buffer;

    public ZeroCopyLinkBuffer withBuffer(Buffer buffer) {
        this.buffer = buffer;
        return this;
    }

    @Override
    public long size() {
        return buffer.length();
    }

    @Override
    public List<ByteBuffer> getBuffers() {
        throw new UnsupportedOperationException("Too costly to call it.");
    }

    @Override
    public List<ByteBuffer> finish() {
        throw new UnsupportedOperationException("Too costly to call it.");
    }

    @Override
    public LinkBuffer writeByte(byte value) throws IOException {
        buffer.writeByte(value);
        return this;
    }

    @Override
    public LinkBuffer writeInt16(int value) throws IOException {
        buffer.writeShort((short) value);
        return this;
    }

    @Override
    public LinkBuffer writeInt16LE(int value) throws IOException {
        buffer.writeByte((byte) value);
        buffer.writeByte((byte) (value >>> 8 & 255));
        return this;
    }

    @Override
    public LinkBuffer writeInt32(int value) throws IOException {
        buffer.writeInt(value);
        return this;
    }

    @Override
    public LinkBuffer writeInt32LE(int value) throws IOException {
        buffer.writeByte((byte) (value & 255));
        buffer.writeByte((byte) (value >>> 8 & 255));
        buffer.writeByte((byte) (value >>> 16 & 255));
        buffer.writeByte((byte) (value >>> 24 & 255));
        return this;
    }

    @Override
    public LinkBuffer writeInt64(long value) throws IOException {
        buffer.writeLong(value);
        return this;
    }

    @Override
    public LinkBuffer writeInt64LE(long value) throws IOException {
        buffer.writeByte((byte) ((int) (value)));
        buffer.writeByte((byte) ((int) (value >>> 8)));
        buffer.writeByte((byte) ((int) (value >>> 16)));
        buffer.writeByte((byte) ((int) (value >>> 24)));
        buffer.writeByte((byte) ((int) (value >>> 32)));
        buffer.writeByte((byte) ((int) (value >>> 40)));
        buffer.writeByte((byte) ((int) (value >>> 48)));
        buffer.writeByte((byte)((int)(value >>> 56)));
        return this;
    }

    @Override
    public LinkBuffer writeVarInt32(int value) throws IOException {
        // TODO too costly operation
        byte[] buf = new byte[5];
        int locPtr = 0;
        while (true)
        {
            if ((value & ~0x7F) == 0)
            {
                buf[locPtr++] = (byte) value;
                // thing;
                buffer.writeBytes(buf, 0, locPtr);
                return this;
            }
            else
            {
                buf[locPtr++] = (byte) ((value & 0x7F) | 0x80);
                value >>>= 7;
            }
        }
    }

    @Override
    public LinkBuffer writeVarInt64(long value) throws IOException {
        // TODO too costly operation
        byte[] buf = new byte[10];
        int locPtr = 0;

        while (true)
        {
            if ((value & ~0x7FL) == 0)
            {
                buf[locPtr++] = (byte) value;
                buffer.writeBytes(buf, 0, locPtr);
                return this;
            }
            else
            {
                buf[locPtr++] = (byte) (((int) value & 0x7F) | 0x80);
                value >>>= 7;
            }
        }
    }

    @Override
    public LinkBuffer writeByteArray(byte[] value, final int offset, final int length) throws IOException {
        // copy in:
        buffer.writeBytes(value, offset, length);
        return this;
    }

    @Override
    public LinkBuffer writeByteBuffer(ByteBuffer buf) {
        ByteBuffer cp = buf.slice();
        buffer.writeFromBuffer(cp);
        return this;
    }
}
