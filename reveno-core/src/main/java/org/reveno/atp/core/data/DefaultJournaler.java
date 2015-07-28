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

import org.reveno.atp.core.api.Journaler;
import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.api.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class DefaultJournaler implements Journaler {

	@Override
	public void writeData(Consumer<Buffer> writer, boolean endOfBatch) {
		requireWriting();

		Channel ch = channel.get();
		ch.write(writer::accept, endOfBatch);

		if (!oldChannel.compareAndSet(ch, ch)) {
			if (oldChannel.get().isOpen())
				closeSilently(oldChannel.get());
			oldChannel.set(ch);

			if (rolledHandler != null) {
				rolledHandler.run();
				rolledHandler = null;
			}
		}
	}

	@Override
	public void startWriting(Channel ch) {
		log.info("Started writing to " + ch);
		
		channel.set(ch);
		oldChannel.set(ch);
		isWriting = true;
	}

	@Override
	public void stopWriting() {
		log.info("Stopped writing to " + channel.get());
		
		isWriting = false;
		closeSilently(channel.get());
		if (oldChannel.get().isOpen())
			closeSilently(oldChannel.get());
	}

	@Override
	public void roll(Channel ch, Runnable rolled) {
		this.rolledHandler = rolled;
		channel.set(ch);
	}
	
	@Override 
	public void destroy() {
		stopWriting();
	}
	
	
	protected void closeSilently(Channel ch) {
		log.info("Closing channel.");
		try {
			ch.close();
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
		}
	}
	
	protected void requireWriting() {
		if (!isWriting)
			throw new RuntimeException("Journaler must be in writing mode.");
	}
	
	
	protected AtomicReference<Channel> channel = new AtomicReference<Channel>();
	protected AtomicReference<Channel> oldChannel = new AtomicReference<Channel>();
	protected volatile Runnable rolledHandler;
	protected volatile boolean isWriting = false;
	protected static final Logger log = LoggerFactory.getLogger(DefaultJournaler.class);
}
