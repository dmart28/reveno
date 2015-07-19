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

package org.reveno.atp.core.serialization;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.api.serialization.TransactionInfoSerializer;
import org.reveno.atp.core.channel.ByteBufferWrapper;
import org.reveno.atp.core.channel.NettyBasedBuffer;
import org.reveno.atp.core.engine.components.SerializersChain;

public class SerializersChainTest {

	@Test
	public void test() {
		List<TransactionInfoSerializer> serializers = new ArrayList<>();
		serializers.add(new ProtostuffSerializer());
		serializers.add(new DefaultJavaSerializer());
		
		SerializersChain chain = new SerializersChain(serializers);
        ByteBufferWrapper buffer = new ByteBufferWrapper(java.nio.ByteBuffer.allocate(1024 * 1024));
		chain.registerTransactionType(User.class);
		
		User user = new User("Artem", 22);
		chain.serializeCommands(Arrays.asList(new Object[] { user }), buffer);

        buffer.getBuffer().flip();

		user = (User)chain.deserializeCommands(buffer).get(0);
		Assert.assertEquals("Artem", user.getName());
		Assert.assertEquals(22, user.getAge());
	}
	
	@Test
	public void testProtostuffFailJavaFin() {
		List<TransactionInfoSerializer> serializers = new ArrayList<>();
		serializers.add(new ProtostuffSerializer());
		serializers.add(new DefaultJavaSerializer());
		
		SerializersChain chain = new SerializersChain(serializers);
        ByteBufferWrapper buffer = new ByteBufferWrapper(java.nio.ByteBuffer.allocate(1024 * 1024));
		chain.registerTransactionType(Empty.class);
		chain.serializeCommands(Arrays.asList(new Object[] { new Empty() }), buffer);
        buffer.getBuffer().flip();
		Empty empty = (Empty)chain.deserializeCommands(buffer).get(0);
		
		Assert.assertNotNull(empty);
	}
	
	public static class FullyEmpty {}
	@SuppressWarnings("serial")
	public static class Empty implements Serializable {}
}
