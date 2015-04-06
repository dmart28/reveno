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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.reveno.atp.api.domain.MutableRepository;
import org.reveno.atp.api.exceptions.EntityNotFoundException;
import org.reveno.atp.core.api.TxRepository;

public class ImmutableModelRepository implements TxRepository {

	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(Class<T> entityType, long id) {
		T entity = repository.get(entityType, id);
		if (isTransaction.get()) {
			if (entity != null && isDeleted(entityType, id))
				return null;
			else if (getAddedEntities(entityType).containsKey(id))
				return (T) getAddedEntities(entityType).get(id);
			else
				throw new EntityNotFoundException(id, entityType);
		} else if (entity != null)
			return entity;
		else 
			throw new EntityNotFoundException(id, entityType);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Collection<T> getAll(Class<T> entityType) {
		return (Collection<T>) getEntities(entityType).values();
	}

	@Override
	public Map<Class<?>, Map<Long, Object>> getAll() {
		if (isTransaction.get()) {
			Map<Class<?>, Map<Long, Object>> map = repository.getAll();
			map.forEach((k,v) -> v.forEach((k1, v1) -> {
				if (!isDeleted(v1.getClass(), k1))
					v.remove(k1);
				getAddedEntities(k).forEach((id, e) -> v.put(id, e));
			}));
			return map;
		} else
			return repository.getAll();
	}
	
	@Override
	public Map<Long, Object> getEntities(Class<?> entityType) {
		Map<Long, Object> entities = repository.getEntities(entityType);

		if (isTransaction.get()) {
			List<Map.Entry<Long, Object>> notRemoved = entities.entrySet()
					.stream().filter(e -> !isDeleted(e.getValue().getClass(), e.getKey()))
					.collect(Collectors.toList());
			notRemoved.addAll(getAddedEntities(entityType).entrySet());
			return notRemoved.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		} else
			return entities;
	}

	@Override
	public <T> T store(long entityId, T entity) {
		if (isTransaction.get()) {
			getAddedEntities(entity.getClass()).put(entityId, entity);
			return entity;
		} else
			return repository.store(entityId, entity);
	}

	@Override
	public Object remove(Class<?> entityType, long entityId) {
		if (isTransaction.get()) {
			if (!removed.containsKey(entityType))
				removed.put(entityType, new HashSet<>());
			removed.get(entityType).add(entityId);
			return repository.get(entityType, entityId);
		} else
			return repository.remove(entityType, entityId);
	}

	@Override
	public void load(Map<Class<?>, Map<Long, Object>> map) {
		if (isTransaction.get()) {
			added.putAll(map);
		} else
			repository.load(map);
	}

	@Override
	public void begin() {
		isTransaction.set(true);
		added.clear();
		removed.clear();
	}

	@Override
	public void commit() {
		added.forEach((k,v) -> v.forEach((i,e) -> repository.store(i, e)));
		removed.forEach((k,v) -> v.forEach(id -> repository.remove(k, id)));
		isTransaction.set(false);
	}

	@Override
	public void rollback() {
		isTransaction.set(false);
	}
	
	protected Map<Long, Object> getAddedEntities(Class<?> entityType) {
		if (!added.containsKey(entityType))
			added.put(entityType, new HashMap<>());
		return added.get(entityType);
	}

	protected boolean isDeleted(Class<?> type, long key) {
		return removed.containsKey(type) && removed.get(type).contains(key);
	}

	public ImmutableModelRepository(MutableRepository underlyingRepository) {
		this.repository = underlyingRepository;
	}

	protected Map<Class<?>, Map<Long, Object>> added = new HashMap<>();
	protected Map<Class<?>, Set<Long>> removed = new HashMap<>();
	protected final MutableRepository repository;
	protected final ThreadLocal<Boolean> isTransaction = new ThreadLocal<Boolean>() {
		protected Boolean initialValue() {
			return false;
		}
	};

}
