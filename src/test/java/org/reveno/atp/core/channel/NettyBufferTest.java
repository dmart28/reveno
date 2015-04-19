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

import java.util.Random;

import org.junit.Assert;

import org.junit.Test;
import org.reveno.atp.core.api.channel.Buffer;

public class NettyBufferTest {

	@Test
	public void test() {
		Buffer buffer = new NettyBasedBuffer();
		byte[] data = new byte[14];
		new Random().nextBytes(data);
		
		buffer.writeInt(5);
		buffer.writeLong(Long.MAX_VALUE);
		buffer.writeBytes(data);
		
		int len = 4 + 8 + 14;
		Assert.assertEquals(len, buffer.length());
		Assert.assertEquals(5, buffer.readInt());
		Assert.assertEquals(Long.MAX_VALUE, buffer.readLong());
		Assert.assertArrayEquals(data, buffer.readBytes(14));
		Assert.assertEquals(len, buffer.getBytes().length);
	}
	
	@Test
	public void testCapacity() {
		Buffer buffer = new NettyBasedBuffer(15, false);
		Assert.assertEquals(15, buffer.capacity());
		Assert.assertEquals(0, buffer.length());
	}
	
}
