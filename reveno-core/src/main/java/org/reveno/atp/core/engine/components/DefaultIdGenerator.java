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

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import org.reveno.atp.api.commands.CommandContext;
import org.reveno.atp.api.transaction.TransactionContext;
import org.reveno.atp.core.api.IdGenerator;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

public class DefaultIdGenerator implements IdGenerator, BiConsumer<DefaultIdGenerator.NextIdTransaction, TransactionContext> {
	
	@Override
	public void context(CommandContext context) {
		this.context = context;
	}

	@Override
	public long next(Class<?> entityType) {
		IdsBundle bundle = context.repo().get(IdsBundle.class, 0L);
		long id = lastIds.getOrDefault(entityType, 0L) + (bundle != null ? bundle.get(entityType) + 1 : 1);
		lastIds.put(entityType, lastIds.getOrDefault(entityType, 0L) + 1);
		
		context.executeTransaction(new NextIdTransaction(entityType, id));
		return id;
	}
	
	@Override
	public void accept(DefaultIdGenerator.NextIdTransaction t, TransactionContext u) {
		lastIds.clear();
		u.repo().store(0, u.repo().has(IdsBundle.class, 0) ? u.repo().get(IdsBundle.class, 0).store(t.entityType, t.id)
				: new IdsBundle().store(t.entityType, t.id));
	}

	protected Object2LongMapEx<Class<?>> lastIds = new Object2LongOpenHashMapEx<>();
	protected CommandContext context;
	
	
	public static class IdsBundle implements Serializable {
		private static final long serialVersionUID = 1L;

		public long get(Class<?> type) {
			return ids.getOrDefault(type, 0L);
		}
		
		public IdsBundle store(Class<?> type, long id) {
			ids.put(type, id);
			return this;
		}
		
		protected Object2LongOpenHashMapEx<Class<?>> ids = new Object2LongOpenHashMapEx<>();
	}
	
	public static class NextIdTransaction implements Serializable {
		public final Class<?> entityType;
		public final long id;
		
		public NextIdTransaction(Class<?> entityType, long id) {
			this.entityType = entityType;
			this.id = id;
		}
	}

	public interface Object2LongMapEx<T> extends Object2LongMap<T> {
		long getOrDefault(T key, long value);
	}

	public static class Object2LongOpenHashMapEx<T> extends Object2LongOpenHashMap<T> implements Object2LongMapEx<T> {

		@Override
		public long getOrDefault(T key, long value) {
			if (this.containsKey(key)) {
				return this.getLong(key);
			} else {
				return value;
			}
		}
	}

}
