package org.reveno.atp.core.data;

import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.api.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;

public class ChannelReader implements Iterable<Buffer> {
	protected static final Logger log = LoggerFactory.getLogger(ChannelReader.class);
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
}
