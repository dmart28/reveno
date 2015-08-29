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
	private Iterator<Channel> channels;

	public ChannelReader(List<Channel> channels) {
		this.channels = channels.iterator();
	}

	@Override
	public Iterator<Buffer> iterator() {
		return new Iterator<Buffer>() {
			private Buffer buffer;
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
					} else {
						return null;
					}
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
