package org.reveno.atp.core.channel;

import org.reveno.atp.core.api.channel.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

import static org.reveno.atp.utils.UnsafeUtils.destroyDirectBuffer;

public class ChannelBuffer implements Buffer {
	protected static final Logger log = LoggerFactory.getLogger(ChannelBuffer.class);
	protected Supplier<ByteBuffer> reader;
	protected Supplier<ByteBuffer> extender;
	protected ByteBuffer buffer;
	protected int sizePosition = -1;
	protected int writerMark = 0;
	protected int nextLimitOnAutoextend = 0;
	protected int startedLimitAfterAutoextend = 0;
	protected boolean intentionalLimit = false;

	public ChannelBuffer(ByteBuffer buffer) {
		this.buffer = buffer;
		this.extender = () -> this.cloneExtended(buffer.limit());
		this.reader = () -> buffer;
	}

	public ChannelBuffer(ByteBuffer buffer, Supplier<ByteBuffer> reader, Supplier<ByteBuffer> extender) {
		this.buffer = buffer;
		this.extender = extender;
		this.reader = reader;
	}

	public ByteBuffer getBuffer() {
		return buffer;
	}

	@Override
	public int readerPosition() {
		return buffer.position();
	}

	@Override
	public int writerPosition() {
		return buffer.position();
	}

	@Override
	public int limit() {
		return buffer.limit();
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
		int r = buffer.remaining();
		if (r == 0 && !intentionalLimit) {
			autoExtend(true);
			r = buffer.remaining();
		}
		return r;
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
		boolean result = buffer.remaining() > 0;
		if (!result) {
			autoExtendIfRequired(1, true);
			result = buffer.remaining() > 0;
		}
		return result;
	}

	@Override
	public void setReaderPosition(int position) {
		this.buffer.position(position);
	}

	@Override
	public void setWriterPosition(int position) {
		this.buffer.position(position);
	}

	public void setLimit(int limit) {
		if (limit > buffer.capacity()) {
			nextLimitOnAutoextend = limit - buffer.capacity();
			return;
		}
		this.buffer.limit(limit);
	}

	@Override
	public void writeByte(byte b) {
		autoExtendIfRequired(1);
		buffer.put(b);
	}

	@Override
	public void writeBytes(byte[] bytes) {
		autoExtendIfRequired(bytes.length);
		buffer.put(bytes);
	}

	@Override
	public void writeBytes(byte[] bytes, int offset, int count) {
		autoExtendIfRequired(bytes.length);
		buffer.put(bytes, offset, count);
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
	public void writeShort(short s) {
		autoExtendIfRequired(2);
		buffer.putShort(s);
	}

	@Override
	public void writeFromBuffer(ByteBuffer b) {
		autoExtendIfRequired(b.limit());
		buffer.put(b);
	}

	@Override
	public ByteBuffer writeToBuffer() {
		return buffer.slice();
	}

	@Override
	public byte readByte() {
		autoExtendIfRequired(1, true);
		return buffer.get();
	}

	@Override
	public byte[] readBytes(int length) {
		autoExtendIfRequired(length, true);
		byte[] b = new byte[length];
		buffer.get(b);
		return b;
	}

	@Override
	public void readBytes(byte[] data, int offset, int length) {
		autoExtendIfRequired(length, true);
		buffer.get(data, offset, length);
	}

	@Override
	public long readLong() {
		autoExtendIfRequired(8, true);
		return buffer.getLong();
	}

	@Override
	public int readInt() {
		autoExtendIfRequired(4, true);
		return buffer.getInt();
	}

	@Override
	public short readShort() {
		autoExtendIfRequired(2, true);
		return buffer.getShort();
	}

	@Override
	public void markReader() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void markWriter() {
		writerMark = buffer.position();
		buffer.mark();
	}

	@Override
	public void resetReader() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void resetWriter() {
		writerMark = -1;
		buffer.reset();
	}

	@Override
	public void limitNext(int count) {
		autoExtendIfRequired(count, true);
		startedLimitAfterAutoextend = buffer.limit();
		setLimit(readerPosition() + count);
		intentionalLimit = true;
	}

	@Override
	public void resetNextLimit() {
		setLimit(startedLimitAfterAutoextend);
		intentionalLimit = false;
	}

	@Override
	public void markSize() {
		sizePosition = buffer.position();
		autoExtendIfRequired(4);
		buffer.putInt(0);
	}

	@Override
	public int sizeMarkPosition() {
		return sizePosition;
	}

	@Override
	public void writeSize() {
		buffer.putInt(sizePosition, buffer.position() - sizePosition - 4);
	}

	public ByteBuffer cloneExtended(int length) {
		ByteBuffer newBuffer = ByteBuffer.allocateDirect(length);
		buffer.flip();
		newBuffer.put(buffer);
		if (writerMark > 0) {
			int oldPos = buffer.position();
			newBuffer.position(writerMark);
			newBuffer.mark();
			newBuffer.position(oldPos);
		}
		buffer.position(buffer.limit());
		buffer.limit(buffer.capacity());
		return newBuffer;
	}

	protected void autoExtendIfRequired(int length) {
		autoExtendIfRequired(length, false);
	}

	protected void autoExtendIfRequired(int length, boolean read) {
		if ((buffer.position() + length) - buffer.limit() > 0) {
			autoExtend(read);
		}
	}

	protected void autoExtend(boolean read) {
		ByteBuffer buf = buffer;
		if (read) {
            buffer = reader.get();
        } else {
            buffer = extender.get();
            if (buffer.position() < sizePosition) {
                sizePosition = 0;
            }
        }
		if (buffer != buf)
            destroyDirectBuffer(buf);
		if (nextLimitOnAutoextend != 0) {
            startedLimitAfterAutoextend = buffer.limit();
            setLimit(nextLimitOnAutoextend);
            nextLimitOnAutoextend = 0;
        }
	}
}
