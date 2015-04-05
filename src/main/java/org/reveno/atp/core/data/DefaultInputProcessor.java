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

import java.io.Closeable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.reveno.atp.core.api.InputProcessor;
import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.api.channel.Channel;
import org.reveno.atp.core.channel.NettyBasedBuffer;
import org.reveno.atp.utils.Preconditions;

public class DefaultInputProcessor implements InputProcessor, Closeable {
	
	@Override
	public void process(Channel channel, final Consumer<Buffer> consumer) {
		Preconditions.checkState(channel.isOpen(), "Channel should be open!");
		
		while (channel.isReadAvailable()) {
			final Buffer buf = new NettyBasedBuffer(false);
			channel.read(buf);
			executor.submit(() -> consumer.accept(buf));
		}
	}
	
	@Override 
	public void close() {
		executor.shutdown();
	}
	
	
	private ExecutorService executor = Executors.newSingleThreadExecutor();

}
