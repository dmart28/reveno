package org.reveno.atp.core.channel;

import org.reveno.atp.api.ChannelOptions;
import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.api.channel.Channel;
import org.reveno.atp.utils.MathUtils;
import org.reveno.atp.utils.MeasureUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.util.function.Consumer;

import static org.reveno.atp.utils.UnsafeUtils.destroyDirectBuffer;

public class FileChannel implements Channel {
	public static final int MAX_VALUE = Integer.MAX_VALUE - 8;
	protected static final ByteBuffer ZERO = java.nio.ByteBuffer.allocate(0);
	private static final Logger log = LoggerFactory.getLogger(FileChannel.class);

	protected final File file;
	protected ChannelWriter writer;
	protected RandomAccessFile raf;
	protected java.nio.channels.FileChannel channel;

	protected ChannelOptions channelOptions = ChannelOptions.BUFFERING_VM;
	protected boolean isPreallocated = false;
	protected int extendDelta = MeasureUtils.kb(128);

	protected long position = 0L;
	protected long size = 0L;
	protected long mmapBufferGeneration = 0;
	protected ByteBuffer buffer = ByteBuffer.allocateDirect(MeasureUtils.kb(32));
	protected ChannelBuffer revenoBuffer;

	@Override
	public long size() {
		if (isPreallocated)
			return size;
		else
			return size0();
	}

	@Override
	public long position() {
		return position;
	}

	@Override
	public void close() {
		try {
			log.info("Closing channel " + file);
			if (channel().isOpen() && writer.isInitialized()) {
				if (channelOptions == ChannelOptions.BUFFERING_MMAP_OS) {
					((MappedByteBuffer)buffer).force();
				} else {
					write(b -> {}, true);
				}
				raf.close();
			}
		} catch (Throwable t) {
			log.error("channel close", t);
		}
		destroyDirectBuffer(buffer);
	}

	@Override
	public boolean isReadAvailable() {
		try {
			return position < raf.length();
		} catch (IOException e) {
			log.error("", e);
			return false;
		}
	}

	@Override
	public Buffer read() {
		if (isOpen()) {
			if (channelOptions != ChannelOptions.BUFFERING_MMAP_OS) {
				read0(buffer, 0);
			}
			return revenoBuffer;
		} else {
			return null;
		}
	}
	
	@Override
	public void write(Consumer<Buffer> visitor, boolean flush) {
		if (!writer.isInitialized())
			writer.init();
		writer.write(visitor, flush);
	}

	@Override
	public boolean isOpen() {
		return channel().isOpen();
	}

	public FileChannel channelOptions(ChannelOptions channelOptions) {
		this.channelOptions = channelOptions;
		return this;
	}

	public FileChannel isPreallocated(boolean isPreallocated) {
		this.isPreallocated = isPreallocated;
		return this;
	}

	public FileChannel extendDelta(int extendDelta) {
		this.extendDelta = extendDelta;
		return this;
	}
	
	public java.nio.channels.FileChannel channel() {
        if (channel == null)
            channel = raf.getChannel();
		return channel;
	}

	public FileChannel init() {
		try {
			if (channelOptions == ChannelOptions.BUFFERING_MMAP_OS && !isPreallocated) {
				throw new IllegalArgumentException("mmap can't be used for non pre-allocated journals.");
			}
			this.raf = new RandomAccessFile(file, mode(channelOptions));

			if (channelOptions == ChannelOptions.BUFFERING_MMAP_OS) {
				destroyDirectBuffer(buffer);
				buffer = mmap(size0());
				revenoBuffer = new ChannelBuffer(buffer,
						() -> mmap(size0() - mmapPos()),
						() -> mmap(Math.max(extendDelta, size - mmapPos())));
			} else {
				revenoBuffer = new ChannelBuffer(buffer,
						() -> read0(buffer, Math.abs(buffer.position() - buffer.limit())),
						this::extendBufferedVm);
			}
			switch (channelOptions) {
				case BUFFERING_VM: writer = new BufferedVMWriter(); break;
				case BUFFERING_OS: writer = new BufferedOSWriter(); break;
				case BUFFERING_MMAP_OS: writer = new BufferedMmapWriter(); break;
				case UNBUFFERED_IO: writer = new UnbufferedIOWriter(); break;
				default: throw new RuntimeException("unknown channel extender.");
			}

			this.size = size0();
		} catch (Throwable e) {
			throw new org.reveno.atp.api.exceptions.FileNotFoundException(file, e);
		}
		return this;
	}

	public FileChannel(File file) {
		this.file = file;
	}

	@Override
	public String toString() {
		return file.getName();
	}

	protected long mmapPos() {
		return mmapBufferGeneration + buffer.position();
	}

	protected long bounded(long l) {
		return Math.min(l, MAX_VALUE);
	}

	protected String mode(ChannelOptions channelOptions) {
		return channelOptions == ChannelOptions.UNBUFFERED_IO ? "rwd" : "rw";
	}

	protected ByteBuffer extendBufferedVm() {
		int newLen = MathUtils.next2n(Math.min(buffer.position() + extendDelta, Integer.MAX_VALUE));
		if (newLen == Integer.MAX_VALUE) {
			write(b -> {}, true);
			return buffer;
		} else {
			return (buffer = revenoBuffer.cloneExtended(newLen));
		}
	}

	protected MappedByteBuffer mmap(long remainSize) {
		remainSize = bounded(remainSize);
		try {
			int pos = buffer.position();
			int offset = revenoBuffer == null || revenoBuffer.sizeMarkPosition() == -1 ? 0 : pos - revenoBuffer.sizeMarkPosition();
			long from = mmapBufferGeneration + pos - offset;
			long count = Math.min(remainSize + offset, MAX_VALUE);
			buffer = channel().map(java.nio.channels.FileChannel.MapMode.READ_WRITE, from, count);
			if (log.isDebugEnabled()) {
				log.debug("Switch mmap over (from:{}, to:{})", from, from + count);
			}
			buffer.position(offset);
			mmapBufferGeneration += pos - offset;
			return (MappedByteBuffer) buffer;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected ByteBuffer read0(ByteBuffer buf, int offset) {
		try {
			buf.clear();
			channel().read(buf, position - offset);
			position += buf.position() - offset;
			buf.flip();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return buf;
	}

	protected void write0(ByteBuffer buf, long size) {
		try {
			channel().write(buf, position);
			position += size;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected long size0() {
		try {
			return channel().size();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	abstract class ChannelWriter {

		abstract void write(Consumer<Buffer> writer, boolean flush);


		public void init() {
			isInitialized = true;
		}

		protected boolean isInitialized = false;
		public boolean isInitialized() {
			return isInitialized;
		}
	}

	class BufferedVMWriter extends ChannelWriter {

		@Override
		public void write(Consumer<Buffer> writer, boolean flush) {
			writer.accept(revenoBuffer);
			buffer = revenoBuffer.getBuffer();
			if (flush && buffer.position() > 0) {
				int size = buffer.position();
				buffer.flip();
				write0(buffer, size);
				buffer.clear();
			} else if (flush) {
				write0(ZERO, 0);
			}
		}
	}

	class BufferedOSWriter extends BufferedVMWriter {
		@Override
		public void write(Consumer<Buffer> writer, boolean flush) {
			super.write(writer, true);
		}
	}

	class BufferedMmapWriter extends ChannelWriter {
		@Override
		public void write(Consumer<Buffer> writer, boolean flush) {
			writer.accept(revenoBuffer);
			position = mmapBufferGeneration + buffer.position();
		}
	}

	class UnbufferedIOWriter extends BufferedVMWriter {
		/**
		 * Difference from BufferedOSWriter is that {@link RandomAccessFile} was
		 * created with options "rwd".
		 *
		 * @param writer
		 * @param flush
		 */
		@Override
		public void write(Consumer<Buffer> writer, boolean flush) {
			super.write(writer, true);
		}
	}
}
