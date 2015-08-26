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

package org.reveno.atp.core.data;

import org.reveno.atp.core.api.Decoder;
import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.api.channel.Channel;
import org.reveno.atp.core.channel.NettyBasedBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ChannelReader implements Iterable<Buffer> {
	private static final long CHUNK_SIZE = 256 * 1024;
	private final Decoder<Buffer> decoder;
	private Iterator<Channel> channels;

	public ChannelReader(Decoder<Buffer> decoder, List<Channel> channels) {
		this.channels = channels.iterator();
		this.decoder = decoder;
	}

	@Override
	public Iterator<Buffer> iterator() {
		return new Iterator<Buffer>() {
			private long chunkPos = 0;
			private Buffer prevBuffer, buffer;
			private Channel channel;

			@Override
			public boolean hasNext() {
				if (buffer == null) {
					buffer = nextBuffer();
					if (buffer == null) {
						return false;
					} else {
						return true;
					}
				} else {
					buffer = nextBuffer();
					return buffer != null;
				}
				// set next MappedByteBuffer chunk
				/*chunkPos += buffer.length();
				T result = null;
				try {
					if ((result = decoder.decode(prevBuffer, buffer)) != null) {
						entry = result;
					}
				} catch (Exception e) {
					buffer.release();
					log.error("decode", e);
				}
				if (prevBuffer != null) {
					prevBuffer.release();
				}
				prevBuffer = buffer;
				buffer = null;
				if (!channels.hasNext()) {
					if (entries != null) {
						return true;
					} else {
						channel.close();
						if (buffer != null)
							buffer.release();
						return false;
					}
				} else {
					if (entries == null)
						entries = new ArrayList<>();
					return true;
				}*/
			}

			private Buffer nextBuffer() {
				try {
					if (channel != null) {
						channel.close();
						channel = null;
					}
					if (channels.hasNext()) {
						channel = channels.next();
						log.info("Processing channel: " + channel);
						prevBuffer = null;
						chunkPos = 0;
					} else {
						return null;
					}
					/*long chunkSize = CHUNK_SIZE;
					if (channel.size() - position < chunkSize) {
						chunkSize = channel.size() - position;
					}
					if (chunkSize == 0) {
						channel.close();
						channel = null;
						return nextBuffer(0);
					}*/
					return channel.read();
				} catch (Throwable e) {
					if (channel != null)
						channel.close();
					throw new RuntimeException(e);
				}
			}

			@Override
			public Buffer next() {
				return buffer;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
	
	protected static final Logger log = LoggerFactory.getLogger(ChannelReader.class);
}
