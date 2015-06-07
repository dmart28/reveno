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

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.reveno.atp.api.domain.RepositoryData;
import org.reveno.atp.api.domain.WriteableRepository;

/*
 * Used for recording which entities were modified on stage of Transaction execution, since
 * we will want that info on next step where we updating Views mappings.
 */
public class RecordingRepository implements WriteableRepository {

	private WriteableRepository underlyingRepo;
	
	public RecordingRepository underlying(WriteableRepository repository) {
		this.underlyingRepo = repository;
		return this;
	}
	
	public RecordingRepository map(Map<Class<?>, Set<Long>> markedRecords) {
		this.markedRecords = markedRecords;
		return this;
	}

	@Override
	public <T> Optional<T> get(Class<T> entityType, long id) {
		Optional<T> result = underlyingRepo.get(entityType, id);
		if (result.isPresent())
			markedRecords.get(entityType).add(id);
		return result;
	}

	@Override
	public <T> Collection<T> getAll(Class<T> entityType) {
		markedRecords.get(entityType).clear();
		markedRecords.get(entityType).addAll(GET_ALL);
		return underlyingRepo.getAll(entityType);
	}

	@Override
	public RepositoryData getData() {
		// TODO put a mark refresh all?
		return underlyingRepo.getData();
	}

	@Override
	public Map<Long, Object> getEntities(Class<?> entityType) {
		markedRecords.get(entityType).clear();
		markedRecords.get(entityType).addAll(GET_ALL);
		return underlyingRepo.getEntities(entityType);
	}

	@Override
	public <T> T store(long entityId, T entity) {
		markedRecords.get(entity.getClass()).add(entityId);
		return underlyingRepo.store(entityId, entity);
	}
	
	@Override
	public <T> T store(long entityId, Class<? super T> type, T entity) {
		markedRecords.get(type).add(entityId);
		return underlyingRepo.store(entityId, type, entity);
	}

	@Override
	public Object remove(Class<?> entityClass, long entityId) {
		markedRecords.get(entityClass).remove(entityId);
		markedRecords.get(entityClass).add(-entityId);
		return underlyingRepo.remove(entityClass, entityId);
	}

	@Override
	public void load(Map<Class<?>, Map<Long, Object>> map) {
		underlyingRepo.load(map);
	}
	
	private Map<Class<?>, Set<Long>> markedRecords;
	@SuppressWarnings("serial")
	public static final Set<Long> GET_ALL = new HashSet<Long>() {{
		add(-1L); add(-3L); add(-5L); add(-10L); add(-101L);
	}};
}
