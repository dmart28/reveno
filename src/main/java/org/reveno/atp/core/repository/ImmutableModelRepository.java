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

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.reveno.atp.api.domain.RepositoryData;
import org.reveno.atp.api.domain.WriteableRepository;
import org.reveno.atp.core.api.TxRepository;
import org.reveno.atp.utils.MapUtils;

@Deprecated
public class ImmutableModelRepository implements TxRepository {
	
	// TODO rewrite class to use collections only on rollback();

	@SuppressWarnings("unchecked")
	@Override
	public <T> Optional<T> get(Class<T> entityType, long id) {
		Optional<T> entity = repository.get(entityType, id);
		if (isTransaction.get()) {
			if (entity.isPresent() && isDeleted(entityType, id))
				return Optional.empty();
			else if (added.containsKey(entityType) && added.get(entityType).containsKey(id))
				return Optional.of((T) added.get(entityType).get(id));
			else
				return entity;
		} else {
			return entity;
		}
	}

	@Override
	public RepositoryData getData() {
		if (isTransaction.get()) {
			Map<Class<?>, Map<Long, Object>> map = repository.getData().data;
			map.forEach((k,v) -> v.forEach((k1, v1) -> {
				if (!isDeleted(k, k1))
					v.remove(k1);
				added.get(k).forEach((id, e) -> v.put(id, e));
			}));
			return new RepositoryData(map);
		} else
			return repository.getData();
	}
	
	@Override
	public Map<Long, Object> getEntities(Class<?> entityType) {
		Map<Long, Object> entities = repository.getEntities(entityType);

		if (isTransaction.get()) {
			Set<Map.Entry<Long, Object>> notRemoved;
			if (removed.size() > 0 && removed.get(entityType).size() > 0) {
				notRemoved = entities.entrySet()
						.stream().filter(e -> !isDeleted(entityType, e.getKey()))
						.collect(Collectors.toSet());
			} else {
				notRemoved = entities.entrySet().stream().collect(Collectors.toSet());
			}
			if (added.size() > 0 && added.get(entityType).size() > 0)
				notRemoved.addAll(added.get(entityType).entrySet());
			return notRemoved.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		} else
			return entities;
	}

	@Override
	public <T> T store(long entityId, T entity) {
		if (isTransaction.get()) {
			added.get(entity.getClass()).put(entityId, entity);
			return entity;
		} else
			return repository.store(entityId, entity);
	}
	
	@Override 
	public <T> T store(long entityId, Class<? super T> type, T entity) {
		if (isTransaction.get()) {
			added.get(type).put(entityId, entity);
			return entity;
		} else
			return repository.store(entityId, type, entity);
	}

	@Override
	public Object remove(Class<?> entityType, long entityId) {
		if (isTransaction.get()) {
			removed.get(entityType).add(entityId);
			return repository.get(entityType, entityId).get();
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
		added.forEach((k,v) -> v.clear());
		removed.forEach((k,v) -> v.clear());
	}

	@SuppressWarnings("unchecked")
	@Override
	public void commit() {
		if (added.size() > 0)
			added.forEach((k,v) -> v.forEach((i,e) -> repository.store(i, (Class<Object>)k, e)));
		if (removed.size() > 0)
			removed.forEach((k,v) -> v.forEach(id -> repository.remove(k, id)));
		isTransaction.set(false);
	}

	@Override
	public void rollback() {
		isTransaction.set(false);
	}
	
	@Override
	public Set<Class<?>> getEntityTypes() {
		return repository.getEntityTypes();
	}

	/*
	 * size checking mainly for performance issues on cases where there is no need to check map
	 */
	protected boolean isDeleted(Class<?> type, long key) {
		return removed.size() > 0 && removed.containsKey(type) && removed.get(type).size() > 0 && removed.get(type).contains(key);
	}

	public ImmutableModelRepository(WriteableRepository underlyingRepository) {
		this.repository = underlyingRepository;
	}

	protected final Map<Class<?>, Map<Long, Object>> added = MapUtils.repositoryMap();
	protected final Map<Class<?>, Set<Long>> removed = MapUtils.repositorySet();
	protected final WriteableRepository repository;
	protected final ThreadLocal<Boolean> isTransaction = new ThreadLocal<Boolean>() {
		protected Boolean initialValue() {
			return false;
		}
	};

}
