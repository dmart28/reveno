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

package org.reveno.atp.core.views;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.reveno.atp.api.query.ViewsMapper;

public class ViewsManager {

	public <E, V> void register(Class<E> entityType, Class<V> viewType, ViewsMapper<E, V> mapper) {
		viewsHandlers.put(entityType, new ViewHandlerHolder<E, V>(viewType, mapper));
		viewsToEntities.put(viewType, entityType);
	}
	
	public boolean hasEntityMap(Class<?> entityType) {
		return viewsHandlers.containsKey(entityType);
	}
	
	public ViewHandlerHolder<?, ?> resolveEntity(Class<?> entityType) {
		return viewsHandlers.get(entityType);
	}
	
	public Class<?> resolveEntityType(Class<?> viewType) {
		return viewsToEntities.get(viewType);
	}
	
	protected Map<Class<?>, ViewHandlerHolder<?, ?>> viewsHandlers = new ConcurrentHashMap<>();
	protected Map<Class<?>, Class<?>> viewsToEntities = new ConcurrentHashMap<>();
	
	public static class ViewHandlerHolder<E, V> {
		public Class<V> viewType;
		public ViewsMapper<E, V> mapper;
		
		public ViewHandlerHolder(Class<V> viewType, ViewsMapper<E, V> mapper) {
			this.viewType = viewType;
			this.mapper = mapper;
		}
	}
	
}
