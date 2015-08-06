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

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import org.reveno.atp.api.domain.RepositoryData;
import org.reveno.atp.api.domain.WriteableRepository;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Used for recording which entities were modified on stage of Transaction execution, since
 * we will want that info on next step where we updating Views mappings.
 * 
 * @author Artem Dmitriev <art.dm.ser@gmail.com>
 * 
 */
public class RecordingRepository implements WriteableRepository {

	private WriteableRepository underlyingRepo;
	
	public RecordingRepository underlying(WriteableRepository repository) {
		this.underlyingRepo = repository;
		return this;
	}
	
	public RecordingRepository map(Map<Class<?>, Long2ObjectLinkedOpenHashMap<Object>> markedRecords) {
		this.markedRecords = markedRecords;
		return this;
	}

	@Override
	public <T> Optional<T> get(Class<T> entityType, long id) {
		Optional<T> result = underlyingRepo.get(entityType, id);
		if (result.isPresent())
			markedRecords.get(entityType).put(id, result.get());
		return result;
	}
	
	@Override
	public <T> boolean has(Class<T> entityType, long id) {
		return underlyingRepo.has(entityType, id);
	}
	
	@Override
	public <T> Optional<T> getClean(Class<T> entityType, long id) {
		return underlyingRepo.getClean(entityType, id);
	}

	@Override
	public RepositoryData getData() {
		RepositoryData data = underlyingRepo.getData();
		data.data.forEach((k,v) -> markedRecords.get(k).putAll(v));
		return data;
	}

	@Override
	public Map<Long, Object> getEntities(Class<?> entityType) {
		Map<Long, Object> result = underlyingRepo.getEntities(entityType);
		markedRecords.get(entityType).putAll(result);
		return result;
	}
	
	@Override
	public Map<Long, Object> getEntitiesClean(Class<?> entityType) {
		return underlyingRepo.getEntitiesClean(entityType);
	}

	@Override
	public <T> T store(long entityId, T entity) {
		markedRecords.get(entity.getClass()).put(entityId, entity);
		return underlyingRepo.store(entityId, entity);
	}
	
	@Override
	public <T> T store(long entityId, Class<? super T> type, T entity) {
		markedRecords.get(type).put(entityId, entity);
		return underlyingRepo.store(entityId, type, entity);
	}

	@Override
	public Object remove(Class<?> entityClass, long entityId) {
		markedRecords.get(entityClass).remove(entityId);
		markedRecords.get(entityClass).put(-entityId, EMPTY);
		return underlyingRepo.remove(entityClass, entityId);
	}

	@Override
	public void load(Map<Class<?>, Map<Long, Object>> map) {
		underlyingRepo.load(map);
	}
	
	@Override
	public Set<Class<?>> getEntityTypes() {
		return underlyingRepo.getEntityTypes();
	}
	
	protected Map<Class<?>, Long2ObjectLinkedOpenHashMap<Object>> markedRecords;
	protected static final Object EMPTY = new Object();
	
}
