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

import org.reveno.atp.api.exceptions.BufferOutOfBoundsException;
import org.reveno.atp.core.api.InputProcessor;
import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.api.channel.Channel;
import org.reveno.atp.core.api.storage.JournalsStorage;
import org.reveno.atp.core.api.storage.JournalsStorage.JournalStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DefaultInputProcessor implements InputProcessor, Closeable {
	
	@Override
	public void process(final long fromVersion, final Consumer<Buffer> consumer, JournalType type) {
		List<Channel> chs = Arrays.asList(stores(fromVersion)).stream().map((js) -> type == JournalType.EVENTS ?
				js.getEventsCommitsAddress() : js.getTransactionCommitsAddress())
				.map(storage::channel)/*.limit(Math.abs(stores().length - 1))*/.collect(Collectors.toList());
		ChannelReader bufferReader = new ChannelReader(chs);
		bufferReader.iterator().forEachRemaining(b -> {
			try {
				while (b.isAvailable()) {
					consumer.accept(b);
				}
			}
			catch (BufferOutOfBoundsException ignored) {
				log.info("End of volume was reached ({})", b.readerPosition());
			}
			catch (Exception e) {
				log.error(e.getMessage(), e);
			}
			finally {
				if (b != null)
					b.release();
			}
		});
	}
	
	@Override 
	public void close() {
	}
	
	public DefaultInputProcessor(JournalsStorage storage) {
		this.storage = storage;
	}
	
	
	protected JournalStore[] stores(long fromVersion) {
		return storage.getStoresAfterVersion(fromVersion);
	}
	
	
	protected JournalsStorage storage;
	protected static final Logger log = LoggerFactory.getLogger(DefaultInputProcessor.class);

}
