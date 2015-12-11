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

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.reveno.atp.api.exceptions.BufferOutOfBoundsException;
import org.reveno.atp.core.api.TransactionCommitInfo;
import org.reveno.atp.core.api.TransactionCommitInfo.Builder;
import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.api.serialization.TransactionInfoSerializer;
import org.reveno.atp.core.serialization.DefaultJavaSerializer;
import org.reveno.atp.core.serialization.ProtostuffSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class SerializersChain implements TransactionInfoSerializer {

	@Override
	public int getSerializerType() {
		return 0;
	}
	
	@Override
	public boolean isRegistered(Class<?> type) {
		return preferedSerializer.isRegistered(type);
	}
	
	@Override
	public void registerTransactionType(Class<?> txDataType) {
		transactionSerializers.forEach(s -> { try { s.registerTransactionType(txDataType);
			} catch (Throwable t) { log.error("registerType", t); }});
	}
	
	@Override
	public void serialize(TransactionCommitInfo info, Buffer buffer) {
		tryTo(buffer, serializer.get().with(buffer, info));
	}
	
	@Override
	public TransactionCommitInfo deserialize(Builder builder, Buffer buffer) {
		return tryToD(buffer, s -> s.deserialize(builder, buffer));
	}
	
	@Override
	public void serializeCommands(List<Object> commands, Buffer buffer) {
		tryTo(buffer, serializer.get().with(buffer, commands));
	}
	
	@Override
	public List<Object> deserializeCommands(Buffer buffer) {
		return tryToD(buffer, s -> s.deserializeCommands(buffer));
	}
	
	@Override
	public void serializeObject(Buffer buffer, Object tc) {
		tryTo(buffer, s -> s.serializeObject(buffer, tc));
	}

	@Override
	public Object deserializeObject(Buffer buffer) {
		return tryToD(buffer, s -> s.deserializeObject(buffer));
	}
	
	@SuppressWarnings("serial")
	public SerializersChain(ClassLoader classLoader) {
		this(new ArrayList<TransactionInfoSerializer>() {{
			add(new ProtostuffSerializer(classLoader));
			add(new DefaultJavaSerializer());
		}});
	}
	
	public SerializersChain(List<TransactionInfoSerializer> transactionsSerializers) {
		this.transactionSerializers = transactionsSerializers;
		this.transactionSerializers.forEach(s -> transactionSerializersMap.put(s.getSerializerType(), s));
		this.preferedSerializer = transactionsSerializers.get(0);
	}
	
	
	protected <T> T tryToD(Buffer buffer, Function<TransactionInfoSerializer, T> f) {
		if (!buffer.isAvailable()) {
			throw new BufferOutOfBoundsException();
		}
		int serializerType = buffer.readInt();
		if (serializerType == 0) {
			throw new BufferOutOfBoundsException();
		}
		TransactionInfoSerializer s = transactionSerializersMap.get(serializerType);
		if (s == null) {
			throw new IllegalArgumentException(String.format("Can't find serializer for %d.", serializerType));
		}
		return f.apply(s);
	}
	
	protected void tryTo(Buffer buffer, Consumer<TransactionInfoSerializer> c) {
		if (!accept(buffer, c, preferedSerializer)) {
			for (int i = 1; i < transactionSerializers.size(); i++) {
				if (accept(buffer, c, transactionSerializers.get(i)))
					return;
			}
			throw new RuntimeException("All serializers failed to serialize.");
		}
	}

	protected boolean accept(Buffer buffer, Consumer<TransactionInfoSerializer> c,
			TransactionInfoSerializer s) {
		try {
			buffer.markWriter();
			buffer.writeInt(s.getSerializerType());
			c.accept(s);
			return true;
		} catch (Throwable tw) {
			log.error("serialize", tw);
			buffer.resetWriter();
			return false;
		}
	}
	
	protected List<TransactionInfoSerializer> transactionSerializers;
	protected TransactionInfoSerializer preferedSerializer;
	protected Int2ObjectMap<TransactionInfoSerializer> transactionSerializersMap = new Int2ObjectOpenHashMap<>();
	protected static final ThreadLocal<Serializer> serializer = new ThreadLocal<Serializer>() {
		@Override
		protected Serializer initialValue() {
			return new Serializer();
		}
	};
	protected static final Logger log = LoggerFactory.getLogger(SerializersChain.class);

	/**
	 * The payment for less garbage in real-time.
	 */
	protected static class Serializer implements Consumer<TransactionInfoSerializer> {
		private Buffer buffer;
		private TransactionCommitInfo info;
		private List<Object> commands;

		public Serializer with(Buffer buffer, TransactionCommitInfo info) {
			this.buffer = buffer;
			this.commands = null;
			this.info = info;
			return this;
		}

		public Serializer with(Buffer buffer, List<Object> commands) {
			this.buffer = buffer;
			this.info = null;
			this.commands = commands;
			return this;
		}

		@Override
		public void accept(TransactionInfoSerializer transactionInfoSerializer) {
			if (commands == null)
				transactionInfoSerializer.serialize(info, buffer);
			else
				transactionInfoSerializer.serializeCommands(commands, buffer);
		}
	}
}
