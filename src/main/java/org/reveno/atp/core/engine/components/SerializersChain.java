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

package org.reveno.atp.core.engine.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.reveno.atp.core.api.TransactionCommitInfo;
import org.reveno.atp.core.api.TransactionCommitInfo.Builder;
import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.api.serialization.TransactionInfoSerializer;
import org.reveno.atp.core.serialization.DefaultJavaSerializer;
import org.reveno.atp.core.serialization.ProtostuffSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerializersChain implements TransactionInfoSerializer {

	@Override
	public int getSerializerType() {
		return 0;
	}
	
	@Override
	public void registerTransactionType(Class<?> txDataType) {
		transactionSerializers.forEach(s -> { try { s.registerTransactionType(txDataType);
			} catch (Throwable t) { log.error("registerType", t); }});
	}
	
	@Override
	public void serialize(TransactionCommitInfo info, Buffer buffer) {
		tryTo(buffer, s -> s.serialize(info, buffer));
	}
	
	@Override
	public TransactionCommitInfo deserialize(Builder builder, Buffer buffer) {
		return tryToD(buffer, s -> s.deserialize(builder, buffer));
	}
	
	@Override
	public void serializeCommands(List<Object> commands, Buffer buffer) {
		tryTo(buffer, s -> s.serializeCommands(commands, buffer));
	}
	
	@Override
	public List<Object> deserializeCommands(Buffer buffer) {
		return tryToD(buffer, s -> s.deserializeCommands(buffer));
	}
	
	@SuppressWarnings("serial")
	public SerializersChain() {
		this(new ArrayList<TransactionInfoSerializer>() {{
			add(new ProtostuffSerializer());
			add(new DefaultJavaSerializer());
		}});
	}
	
	public SerializersChain(List<TransactionInfoSerializer> transactionsSerializers) {
		this.transactionSerializers = transactionsSerializers;
		this.transactionSerializers.forEach(s -> transactionSerializersMap.put(s.getSerializerType(), s));
	}
	
	
	protected <T> T tryToD(Buffer buffer, Function<TransactionInfoSerializer, T> f) {
		buffer.mark();
		int serializerType = buffer.readInt();
		TransactionInfoSerializer s = transactionSerializersMap.get(serializerType);
		if (s == null) {
			buffer.reset();
			throw new IllegalArgumentException(String.format("Can't find serializer for %d.", serializerType));
		}
		return f.apply(s);
	}
	
	protected void tryTo(Buffer buffer, Consumer<TransactionInfoSerializer> c) {
		for (TransactionInfoSerializer s : transactionSerializers) {
			try {
				buffer.mark();
				buffer.writeInt(s.getSerializerType());
				c.accept(s);
				return;
			} catch (Throwable tw) {
				log.error("serialize", tw);
				buffer.reset();
			}
		}
		throw new RuntimeException("All serializers failed to serialize.");
	}
	
	protected List<TransactionInfoSerializer> transactionSerializers;
	protected Map<Integer, TransactionInfoSerializer> transactionSerializersMap = new HashMap<>();
	protected static final Logger log = LoggerFactory.getLogger(SerializersChain.class);
}
