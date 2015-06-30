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

package org.reveno.atp.core.channel;

import static org.reveno.atp.utils.UnsafeUtils.destroyDirectBuffer;

import java.nio.ByteBuffer;

import org.reveno.atp.core.api.channel.Buffer;

/**
 * Not super correct currently - we should use separate reader and writer indexes.
 * 
 * But since for now it is only used for writing as wrapper in FileChannel - let it be.
 * 
 * @author Artem Dmitriev <art.dm.ser@gmail.com>
 *
 */
public class ByteBufferWrapper implements Buffer {
	
	protected ByteBuffer buffer; 
	public ByteBuffer getBuffer() {
		return buffer;
	}
	
	public ByteBufferWrapper(ByteBuffer buffer) {
		this.buffer = buffer;
	}

	@Override
	public byte[] getBytes() {
		return readBytes();
	}

	@Override
	public long capacity() {
		return buffer.capacity();
	}

	@Override
	public int length() {
		return buffer.capacity();
	}

	@Override
	public int remaining() {
		return buffer.remaining();
	}

	@Override
	public void clear() {
		buffer.clear();
	}

	@Override
	public void release() {
		destroyDirectBuffer(buffer);
	}

	@Override
	public boolean isAvailable() {
		return buffer.remaining() > 0;
	}

	@Override
	public void writeBytes(byte[] bytes) {
		autoExtendIfRequired(bytes.length);
		buffer.put(bytes);
	}

	@Override
	public void writeLong(long value) {
		autoExtendIfRequired(8);
		buffer.putLong(value);
	}

	@Override
	public void writeInt(int value) {
		autoExtendIfRequired(4);
		buffer.putInt(value);
	}

	@Override
	public void writeFromBuffer(ByteBuffer b) {
		buffer.put(b);
	}

	@Override
	public void writeFromBuffer(Buffer b) {
		buffer.put(b.getBytes());
	}

	@Override
	public byte[] readBytes() {
		byte[] b = new byte[buffer.remaining()];
		buffer.get(b);
		return b;
	}

	@Override
	public byte[] readBytes(int length) {
		byte[] b = new byte[length];
		buffer.get(b);
		return b;
	}

	@Override
	public void readBytes(byte[] data, int offset, int length) {
		buffer.get(data, offset, length);
	}

	@Override
	public long readLong() {
		return buffer.getLong();
	}

	@Override
	public int readInt() {
		return buffer.getInt();
	}

	@Override
	public void mark() {
		buffer.mark();
	}

	@Override
	public void reset() {
		buffer.reset();
	}
	
	protected void autoExtendIfRequired(int length) {
		if (buffer.position() + length > buffer.capacity()) {
			ByteBuffer newBuffer = ByteBuffer.allocateDirect((int)((buffer.position() + length) * 1.5));
			buffer.flip();
			newBuffer.put(buffer);
			destroyDirectBuffer(buffer);
			buffer = newBuffer;
		}
	}

}
