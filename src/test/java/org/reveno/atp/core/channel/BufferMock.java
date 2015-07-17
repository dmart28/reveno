package org.reveno.atp.core.channel;

import java.nio.ByteBuffer;

import org.reveno.atp.core.api.channel.Buffer;

import static org.reveno.atp.utils.BinaryUtils.*;

public class BufferMock implements Buffer {
	
	private byte[] bytes = new byte[0];
	private int pos = 0;
	
	public BufferMock() {
	}
	
	public BufferMock(byte[] bytes) {
		this.bytes = bytes;
	}

	@Override
	public byte[] getBytes() {
		return bytes;
	}

	@Override
	public int length() {
		return bytes.length;
	}
	
	@Override
	public boolean isAvailable() {
		return bytes.length > 0;
	}

	@Override
	public void clear() {
		pos = 0;
		bytes = new byte[0];
	}
	
	@Override
	public void release() {
	}

	@Override
	public void writeBytes(byte[] b) {
		byte[] c = new byte[bytes.length + b.length];
		System.arraycopy(bytes, 0, c, 0, bytes.length);
		System.arraycopy(b, 0, c, bytes.length, b.length);
		this.bytes = c;
	}

	@Override
	public void writeLong(long value) {
		byte[] sizeBuffer = new byte[8];
		longToBytes(value, sizeBuffer);
		writeBytes(sizeBuffer);
	}

	@Override
	public void writeInt(int value) {
		writeBytes(intToBytes(value));
	}

	@Override
	public void writeFromBuffer(ByteBuffer buffer) {
		byte[] b = new byte[buffer.remaining()];
		buffer.get(b, 0, b.length);
		
		writeBytes(b);
	}

	@Override
	public byte[] readBytes() {
		pos += bytes.length;
		return bytes;
	}

	@Override
	public byte[] readBytes(int length) {
		byte[] data = new byte[length];
		System.arraycopy(bytes, pos, data, 0, length);
		pos += length;
		return data;
	}

	@Override
	public long readLong() {
		return bytesToLong(readBytes(8));
	}

	@Override
	public int readInt() {
		return bytesToInt(readBytes(4));
	}
	
	@Override
	public void mark() {
	}

	@Override
	public void reset() {
	}

	@Override
	public void writeFromBuffer(Buffer buffer) {
	}

	@Override
	public long capacity() {
		return length();
	}

	@Override
	public int remaining() {
		return bytes.length;
	}

	@Override
	public void readBytes(byte[] data, int offset, int length) {
		
	}
}
