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

import org.reveno.atp.api.ChannelOptions;
import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.api.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.util.function.Consumer;

import static org.reveno.atp.utils.MeasureUtils.mb;
import static org.reveno.atp.utils.UnsafeUtils.destroyDirectBuffer;

public class FileChannel implements Channel {

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
			return channel().position() < raf.length();
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
	
	public java.nio.channels.FileChannel channel() {
        if (channel == null)
            channel = raf.getChannel();
		return channel;
	}

    public FileChannel(File file, ChannelOptions channelOptions) {
        this(file, channelOptions, false);
    }
	
	public FileChannel(File file, ChannelOptions channelOptions, boolean isPreallocated) {
		try {
			if (channelOptions == ChannelOptions.BUFFERING_MMAP_OS && !isPreallocated) {
				throw new IllegalArgumentException("mmap can't be used for non pre-allocated journals.");
			}

			this.file = file;
			this.raf = new RandomAccessFile(file, mode(channelOptions));

			if (channelOptions == ChannelOptions.BUFFERING_MMAP_OS) {
				destroyDirectBuffer(buffer);
				buffer = mmap(size0());
				revenoBuffer = new ChannelBuffer(buffer, b -> { return mmap(size0() - lastPos()); },
						(p,b) -> mmap(p, true));
			} else {
				revenoBuffer = new ChannelBuffer(buffer, (Consumer<ByteBuffer>) b -> read0(b, Math.abs(b.position() - b.limit())),
						(p,b) -> revenoBuffer.extendBuffer(p));
			}
			switch (channelOptions) {
				case BUFFERING_VM: writer = new BufferedVMWriter(); break;
				case BUFFERING_OS: writer = new BufferedOSWriter(); break;
				case BUFFERING_MMAP_OS: writer = new BufferedMmapWriter(); break;
				case UNBUFFERED_IO: writer = new UnbufferedIOWriter(); break;
				default: throw new RuntimeException("unknown channel extender.");
			}

			this.channelOptions = channelOptions;
			this.size = size0();
			this.isPreallocated = isPreallocated;
		} catch (Throwable e) {
			throw new org.reveno.atp.api.exceptions.FileNotFoundException(file, e);
		}
	}

	@Override
	public String toString() {
		return file.getName();
	}

	protected long lastPos() {
		return mmapBufferGeneration + buffer.position();
	}

	protected long bounded(long l) {
		return Math.min(l, MAX_VALUE);
	}

	protected String mode(ChannelOptions channelOptions) {
		return channelOptions == ChannelOptions.UNBUFFERED_IO ? "rwd" : "rw";
	}

	protected MappedByteBuffer mmap(long remainSize) {
		return mmap(remainSize, false);
	}

	protected MappedByteBuffer mmap(long remainSize) {
		remainSize = bounded(remainSize);
		try {
			if (log.isDebugEnabled()) {
				log.debug("Switch mmap over (pos:{}, size:{})", position, remainSize);
			}
			int pos = buffer.position();
			boolean extend = pos + remainSize > size;
			buffer = channel().map(
					java.nio.channels.FileChannel.MapMode.READ_WRITE,
					extend ? 0 : mmapBufferGeneration + pos, extend ? pos + remainSize : remainSize);
			if (extend) {
				buffer.
			}
			mmapBufferGeneration += pos;
			return (MappedByteBuffer) buffer;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected void read0(ByteBuffer buf, int offset) {
		try {
			buf.clear();
			channel().read(buf, position - offset);
			position += buf.position() - offset;
			buf.flip();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
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
	
	protected final File file;
	protected final ChannelWriter writer;
	protected RandomAccessFile raf;
    protected java.nio.channels.FileChannel channel;

	protected long position = 0L;
	protected long size = 0L;
	protected int mmapBufferGeneration = 0;
	protected ChannelOptions channelOptions;
	protected boolean isPreallocated = false;
	protected ByteBuffer buffer = ByteBuffer.allocateDirect(mb(1));
	protected ChannelBuffer revenoBuffer;

	protected static final ByteBuffer ZERO = java.nio.ByteBuffer.allocate(0);
	private static final Logger log = LoggerFactory.getLogger(FileChannel.class);
	public static final int MAX_VALUE = Integer.MAX_VALUE - 8;


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
