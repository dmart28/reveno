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
import org.reveno.atp.utils.UnsafeUtils;
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
	
	public static final int PAGE_SIZE = UnsafeUtils.getUnsafe().pageSize();
	public static final byte[] BLANK_PAGE = new byte[PAGE_SIZE];

	@Override
	public long size() {
		if (isOpen())
			return size0();
		else
			return 0;
	}

	@Override
	public long position() {
		return position;
	}

	@Override
	public void close() {
		try {
			if (channel().isOpen()) {
				write(b -> {}, true);
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
	public void read(Buffer data) {
		if (isOpen()) {
			buffer.clear();
			read(buffer);
			buffer.flip();
			
			data.writeFromBuffer(buffer);
			buffer.rewind();
		}
	}
	
	@Override
	public void write(Consumer<Buffer> visitor, boolean flush) {
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
	
	protected void write0(ByteBuffer buf, long size) {
		try {
			channel().write(buf, position);
            position += size;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	protected void read(ByteBuffer buf) {
		try {
			channel().read(buf);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

    public FileChannel(File file, ChannelOptions channelOptions) {
        this(file, channelOptions, 0L);
    }
	
	public FileChannel(File file, ChannelOptions channelOptions, long size) {
		try {
			if (channelOptions == ChannelOptions.BUFFERING_MMAP_OS && size == 0) {
				throw new IllegalArgumentException("mmap can't be used for non pre-allocated journals.");
			}

			this.file = file;
			this.raf = new RandomAccessFile(file, mode(channelOptions));

            if (size != 0) {
                preallocateFiles(file,size, mode(channelOptions));
            }
			if (channelOptions == ChannelOptions.BUFFERING_MMAP_OS) {
				destroyDirectBuffer(buffer);
				buffer = mmap(0, Math.min(size0(), Integer.MAX_VALUE));
				revenoBuffer = new ByteBufferWrapper(buffer, p -> mmap(p, Math.min(size0() - p, Integer.MAX_VALUE)));
			}
			switch (channelOptions) {
				case BUFFERING_VM: writer = new BufferedVMWriter(); break;
				case BUFFERING_OS: writer = new BufferedOSWriter(); break;
				case BUFFERING_MMAP_OS: writer = new BufferedMmapWriter(); break;
				case UNBUFFERED_IO: writer = new UnbufferedIOWriter(); break;
				default: throw new RuntimeException("unknown channel writer.");
			}
			writer.init();
		} catch (Throwable e) {
			throw new org.reveno.atp.api.exceptions.FileNotFoundException(file, e);
		}
	}

	@Override
	public String toString() {
		return file.getName();
	}

	protected String mode(ChannelOptions channelOptions) {
		return channelOptions == ChannelOptions.UNBUFFERED_IO ? "rwd" : "rw";
	}

	private MappedByteBuffer mmap(Integer p, long remainSize) {
		try {
			MappedByteBuffer mb = channel().map(
					java.nio.channels.FileChannel.MapMode.READ_WRITE,
					p, remainSize);
			mb.putInt(0, mb.capacity() - 4);
			return mb;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private long size0() {
		try {
			return channel().size();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

    protected void preallocateFiles(File file, long size, String mode) throws IOException {
        log.info("Preallocating started.");
        for (long i = 0; i < size; i += PAGE_SIZE) {
            raf.write(BLANK_PAGE, 0, PAGE_SIZE);
        }
        log.info("Preallocating finished.");
        raf.close();

        this.raf = new RandomAccessFile(file, mode);
        this.raf.seek(0);
        channel().position(0);
    }
	
	protected final File file;
	protected final ChannelWriter writer;
	protected RandomAccessFile raf;
    protected java.nio.channels.FileChannel channel;

	protected long position = 0L;
	protected ByteBuffer buffer = ByteBuffer.allocateDirect(mb(1));
	protected ByteBufferWrapper revenoBuffer = new ByteBufferWrapper(buffer);

	protected static final ByteBuffer ZERO = java.nio.ByteBuffer.allocate(0);
	private static final Logger log = LoggerFactory.getLogger(FileChannel.class);


	interface ChannelWriter {
		void init();

		void write(Consumer<Buffer> writer, boolean flush);
	}

	class BufferedVMWriter implements ChannelWriter {
		@Override
		public void init() {
			revenoBuffer.writeInt(1);
		}

		@Override
		public void write(Consumer<Buffer> writer, boolean flush) {
			writer.accept(revenoBuffer);
			buffer = revenoBuffer.getBuffer();
			if (flush && buffer.position() > 4) {
				int size = buffer.position();
				buffer.putInt(0, size - 4).flip();
				write0(buffer, size);
				buffer.clear();
				revenoBuffer.writeInt(1);
			} else if (flush) {
				write0(ZERO, 0);
			}
		}
	}

	class BufferedOSWriter extends BufferedVMWriter {
		@Override
		public void init() {
			revenoBuffer.writeInt(1);
		}

		@Override
		public void write(Consumer<Buffer> writer, boolean flush) {
			super.write(writer, true);
		}
	}

	class BufferedMmapWriter implements ChannelWriter {
		@Override
		public void init() {
			revenoBuffer.writeInt(buffer.capacity() - 4);
		}

		@Override
		public void write(Consumer<Buffer> writer, boolean flush) {
			writer.accept(revenoBuffer);
			position = buffer.position();
		}
	}

	class UnbufferedIOWriter extends BufferedVMWriter {
		@Override
		public void init() {
			revenoBuffer.writeInt(1);
		}

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
