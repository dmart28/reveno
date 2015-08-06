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
import org.reveno.atp.api.domain.RepositoryData;
import org.reveno.atp.api.domain.WriteableRepository;
import org.reveno.atp.utils.MapUtils;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@SuppressWarnings("unchecked")
public class HashMapRepository implements WriteableRepository {
	
	@Override
	public <T> Optional<T> get(Class<T> entityType, long id) {
		T entity = (T) map.get(entityType).get(id);
		
		return Optional.ofNullable(entity);
	}
	
	@Override
	public <T> boolean has(Class<T> entityType, long id) {
		return get(entityType, id).isPresent();
	}
	
	@Override
	public <T> Optional<T> getClean(Class<T> entityType, long id) {
		return get(entityType, id);
	}

	@Override
	public RepositoryData getData() {
		Map<Class<?>, Map<Long, Object>> data = MapUtils.repositoryMap();
		map.forEach((k,v) -> data.get(k).putAll(v));
		return new RepositoryData(data);
	}

	@Override
	public <T> T store(long entityId, T entity) {
		map.get(entity.getClass()).put(entityId, entity);
		return entity;
	}
	
	@Override
	public <T> T store(long entityId, Class<? super T> type, T entity) {
		map.get(type).put(entityId, entity);
		return entity;
	}

	@Override
	public Object remove(Class<?> entityClass, long entityId) {
		Long2ObjectOpenHashMap<Object> data = map.get(entityClass);
		if (data != null)
			return data.remove(entityId);
		return null;
	}

	@Override
	public void load(Map<Class<?>, Map<Long, Object>> data) {
		data.forEach((k,v) -> map.put(k, new Long2ObjectOpenHashMap<>(v)));
	}
	
	@Override
	public Map<Long, Object> getEntities(Class<?> entityType) {
		return map.get(entityType);
	}
	
	@Override
	public Map<Long, Object> getEntitiesClean(Class<?> entityType) {
		return getEntities(entityType);
	}
	
	@Override
	public Set<Class<?>> getEntityTypes() {
		return map.keySet();
	}
	
	
	public HashMapRepository() {
		this(524288, 0.75f);
	}
	
	public HashMapRepository(int capacity, float loadFactor) {
		map = MapUtils.fastRepo(capacity, loadFactor);
		this.capacity = capacity;
		this.loadFactor = loadFactor;
	}
	
	protected Map<Class<?>, Long2ObjectOpenHashMap<Object>> map;
	protected int capacity;
	protected float loadFactor;
	
}
