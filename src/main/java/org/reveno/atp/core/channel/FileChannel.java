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

import static org.reveno.atp.utils.BinaryUtils.longToBytes;
import static org.reveno.atp.utils.MeasureUtils.mb;
import static org.reveno.atp.utils.UnsafeUtils.destroyDirectBuffer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.api.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileChannel implements Channel {

	@Override
	public long size() {
		if (isOpen())
			try {
				return channel().size();
			} catch (IOException e) {
				log.error("size", e);
				return 0;
			}
		else
			return 0;
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
	public void write(Consumer<Buffer> writer, boolean flush) {
		writer.accept(revenoBuffer);
		buffer = revenoBuffer.getBuffer();
		if (flush && buffer.position() > 0) {
			try {
				raf.write(longToBytes(buffer.position()));
			} catch (IOException e) {
				log.error("write", e);
				throw new RuntimeException(e);
			}
			buffer.flip();
			write(buffer);
			buffer.clear();
		}
	}

	@Override
	public boolean isOpen() {
		return channel().isOpen();
	}
	
	public java.nio.channels.FileChannel channel() {
		return raf.getChannel();
	}
	
	protected long currentPosition() {
		try {
			return channel().position();
		} catch (Throwable t) {
			log.error("", t);
			return 0;
		}
	}
	
	protected void write(ByteBuffer buf) {
		try {
			channel().write(buf);
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
	
	public FileChannel(File file, String mode) {
		try {
			this.file = file;
			this.raf = new RandomAccessFile(file, mode);
		} catch (FileNotFoundException e) {
			throw new org.reveno.atp.api.exceptions.FileNotFoundException(file);
		}
	}
	
	public FileChannel(String filename, String mode) {
		try {
			this.file = new File(filename);
			this.raf = new RandomAccessFile(filename, mode);
		} catch (FileNotFoundException e) {
			throw new org.reveno.atp.api.exceptions.FileNotFoundException(new File(filename));
		}
	}
	
	@Override
	public String toString() {
		return file.getName();
	}
	
	private final File file;
	private final RandomAccessFile raf;
	private ByteBuffer buffer = ByteBuffer.allocateDirect(mb(1));
	private ByteBufferWrapper revenoBuffer = new ByteBufferWrapper(buffer);
	
	
	private static final Logger log = LoggerFactory.getLogger(FileChannel.class);
}
