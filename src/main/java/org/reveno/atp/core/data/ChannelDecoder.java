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

import static org.reveno.atp.utils.BinaryUtils.bytesToLong;

import org.reveno.atp.core.api.Decoder;
import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.channel.NettyBasedBuffer;

public class ChannelDecoder implements Decoder<Buffer> {

	@Override
	public Buffer decode(Buffer prevBuffer, Buffer buffer) {
		if (prevBuffer != null && prevBuffer.remaining() > 0) {
			int remains = prevBuffer.remaining();
			if (lastSize == 0L) {
				byte[] lengthBytes = prevBuffer.readBytes(8);
				buffer.readBytes(lengthBytes, remains, 8 - remains);
				lastSize = bytesToLong(lengthBytes);
				if (lastSize == 0) return null;
				return readBuffer(buffer);
			} else {
				byte[] data = prevBuffer.readBytes((int)lastSize);
				if (lastSize == 0) return null;
				buffer.readBytes(data, remains, (int)(lastSize - remains));
				NettyBasedBuffer b = new NettyBasedBuffer((int)lastSize, false);
				b.writeBytes(data);
				return b;
			}
		} else {
			if (buffer.remaining() < 8) {
				lastSize = 0L;
				return null;
			}
			lastSize = buffer.readLong();
			if (lastSize == 0) return null;
			return readBuffer(buffer);
		}
	}

	protected Buffer readBuffer(Buffer buffer) {
		if (lastSize <= buffer.remaining()) {
			NettyBasedBuffer b = new NettyBasedBuffer((int)lastSize, false);
			byte[] data = buffer.readBytes((int)lastSize);
			b.writeBytes(data);
			return b;
		} else 
			return null;
	}
	
	private long lastSize = 0L;

}
