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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.reveno.atp.api.commands.CommandContext;
import org.reveno.atp.api.transaction.TransactionContext;
import org.reveno.atp.core.api.IdGenerator;

public class DefaultIdGenerator implements IdGenerator, BiConsumer<DefaultIdGenerator.NextIdTransaction, TransactionContext> {
	
	@Override
	public void context(CommandContext context) {
		this.context = context;
	}

	@Override
	public long next(Class<?> entityType) {
		Optional<IdsBundle> bundle = context.repository().get(IdsBundle.class, 0L);
		long id = lastIds.getOrDefault(entityType, 0L) + (bundle.isPresent() ? bundle.get().get(entityType) + 1 : 1);
		lastIds.put(entityType, lastIds.getOrDefault(entityType, 0L) + 1);
		
		context.executeTransaction(new NextIdTransaction(entityType, id));
		return id;
	}
	
	@Override
	public void accept(DefaultIdGenerator.NextIdTransaction t, TransactionContext u) {
		lastIds.clear();
		u.repository().store(0, u.repository().get(IdsBundle.class, 0).orElse(new IdsBundle()).store(t.entityType, t.id));
	}

	protected Map<Class<?>, Long> lastIds = new HashMap<>();
	protected CommandContext context;
	
	
	public static class IdsBundle {
		public long get(Class<?> type) {
			return ids.getOrDefault(type, 1L);
		}
		
		public IdsBundle store(Class<?> type, long id) {
			ids.put(type, id);
			return this;
		}
		
		protected Map<Class<?>, Long> ids = new HashMap<>();
	}
	
	public static class NextIdTransaction {
		public final Class<?> entityType;
		public final long id;
		
		public NextIdTransaction(Class<?> entityType, long id) {
			this.entityType = entityType;
			this.id = id;
		}
	}
	
}
