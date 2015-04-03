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

import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.api.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileChannel implements Channel {

	@Override
	public void close() throws IOException {
		raf.close();
		destroyDirectBuffer(buffer);
	}

	@Override
	public void recoverCorrupted(long position) {
		try {
			if (isOpen())
				raf.setLength(position + 1);
		} catch (IOException e) {
			log.error("recoverCorrupted", e);
		}
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
			buffer.rewind();
			read(buffer);
			buffer.flip();
			
			data.writeFromBuffer(buffer);
		}
	}

	@Override
	public void write(Buffer data) {
		if (isOpen())
			write(data.getBytes());
	}
	
	@Override 
	public void writeIndex() {
		if (isOpen())
			write(longToBytes(currentPosition()));
	}

	@Override
	public boolean isOpen() {
		return channel().isOpen();
	}
	
	protected void write(byte[] data) {
		if (data.length > buffer.capacity()) {
			destroyDirectBuffer(buffer);
			buffer = ByteBuffer.allocateDirect(data.length + 1);
		}
		buffer.put(data);
		buffer.flip();
		write(buffer);
		buffer.clear();
	}
	
	protected java.nio.channels.FileChannel channel() {
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
			raf = new RandomAccessFile(file, mode);
		} catch (FileNotFoundException e) {
			throw new org.reveno.atp.api.exceptions.FileNotFoundException(file);
		}
	}
	
	public FileChannel(String filename, String mode) {
		try {
			raf = new RandomAccessFile(filename, mode);
		} catch (FileNotFoundException e) {
			throw new org.reveno.atp.api.exceptions.FileNotFoundException(new File(filename));
		}
	}
	
	private RandomAccessFile raf;
	private ByteBuffer buffer = ByteBuffer.allocateDirect(mb(1));
	
	private static final Logger log = LoggerFactory.getLogger(FileChannel.class);
}
