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

package org.reveno.atp.core.repository;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.reveno.atp.api.domain.RepositoryData;
import org.reveno.atp.api.domain.WriteableRepository;

public class HashMapRepository implements WriteableRepository {

	@SuppressWarnings("unchecked")
	@Override
	public <T> Optional<T> get(Class<T> entityType, long id) {
		T entity = (T) getEntities(entityType).get(id);
		
		return Optional.ofNullable(entity);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <T> Collection<T> getAll(Class<T> entityType) {
		return (Collection<T>) Collections.unmodifiableCollection(getEntities(entityType).values());
	}

	@Override
	public RepositoryData getData() {
		return new RepositoryData(Collections.unmodifiableMap(map));
	}

	@Override
	public <T> T store(long entityId, T entity) {
		getEntities(entity.getClass()).put(entityId, entity);
		return entity;
	}
	
	@Override
	public <T> T store(long entityId, Class<? super T> type, T entity) {
		getEntities(type).put(entityId, entity);
		return entity;
	}

	@Override
	public Object remove(Class<?> entityClass, long entityId) {
		if (map.containsKey(entityClass))
			return map.get(entityClass).remove(entityId);
		return null;
	}

	@Override
	public void load(Map<Class<?>, Map<Long, Object>> map) {
		this.map.putAll(map);
	}
	
	@Override
	public Map<Long, Object> getEntities(Class<?> entityType) {
		Map<Long, Object> result = map.get(entityType);
		if (result == null)
			map.put(entityType, (result = new Long2ObjectOpenHashMap<Object>(capacity)));
		return result;
	}
	
	
	public HashMapRepository() {
		this(524288, 0.75f);
	}
	
	public HashMapRepository(int capacity, float loadFactor) {
		this.map = new ConcurrentHashMap<>(capacity, loadFactor, 32);
		this.capacity = capacity;
		this.loadFactor = loadFactor;
	}
	
	protected Map<Class<?>, Map<Long, Object>> map;
	protected int capacity;
	protected float loadFactor;
	
}
