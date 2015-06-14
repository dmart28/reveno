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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.reveno.atp.core.api.Decoder;
import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.api.channel.Channel;
import org.reveno.atp.core.channel.NettyBasedBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelReader<T> implements Iterable<List<T>> {
	private static final long CHUNK_SIZE = 256 * 1024;
	private final Decoder<T> decoder;
	private Iterator<Channel> channels;

	public ChannelReader(Decoder<T> decoder, List<Channel> channels) {
		this.channels = channels.iterator();
		this.decoder = decoder;
	}

	@Override
	public Iterator<List<T>> iterator() {
		return new Iterator<List<T>>() {
			private List<T> entries;
			private long chunkPos = 0;
			private Buffer prevBuffer, buffer;
			private Channel prevChannel, channel;

			@Override
			public boolean hasNext() {
				if (buffer == null || !buffer.isAvailable()) {
					buffer = nextBuffer(chunkPos);
					if (buffer == null) {
						return false;
					}
					if (prevChannel != channel) {
						prevBuffer = null;
						prevChannel = channel;
					}
				}
				// set next MappedByteBuffer chunk
				chunkPos += buffer.length();
				T result = null;
				while ((result = decoder.decode(prevBuffer, buffer)) != null) {
					if (entries == null) {
						entries = new ArrayList<T>();
					}
					entries.add(result);
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
						return false;
					}
				} else {
					if (entries == null)
						entries = new ArrayList<>();
					return true;
				}
			}

			private Buffer nextBuffer(long position) {
				try {
					if (channel == null || channel.size() == position) {
						if (channel != null) {
							channel.close();
							channel = null;
						}
						if (channels.hasNext()) {
							channel = channels.next();
							log.info("Processing channel: " + channel);
							chunkPos = 0;
							position = 0;
						} else {
							return null;
						}
					}
					long chunkSize = CHUNK_SIZE;
					if (channel.size() - position < chunkSize) {
						chunkSize = channel.size() - position;
					}
					if (chunkSize == 0) {
						return nextBuffer(0);
					}
					Buffer buffer = new NettyBasedBuffer((int) chunkSize, false);
					channel.read(buffer);
					return buffer;
				} catch (Throwable e) {
					channel.close();
					throw new RuntimeException(e);
				}
			}

			@Override
			public List<T> next() {
				List<T> res = entries;
				entries = null;
				return res;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
	
	protected static final Logger log = LoggerFactory.getLogger(ChannelReader.class);
}
