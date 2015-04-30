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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.reveno.atp.api.domain.WriteableRepository;
import org.reveno.atp.core.api.TxRepository;
import org.reveno.atp.core.serialization.protostuff.InputOutputHolder;
import org.reveno.atp.utils.MapUtils;

import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;

public class MutableModelRepository implements TxRepository {

	@Override
	public <T> T store(long entityId, T entity) {
		repository.store(entityId, entity);
		if (isTransaction.get() && entity != null)
			saveEntityState(entityId, entity, EntityRecoveryState.REMOVE);
		return entity;
	}
	
	@Override
	public <T> T store(long entityId, Class<? super T> type, T entity) {
		repository.store(entityId, type, entity);
		if (isTransaction.get() && entity != null)
			saveEntityState(entityId, entity, EntityRecoveryState.REMOVE);
		return entity;
	}

	@Override
	public Object remove(Class<?> entityClass, long entityId) {
		Object entity = repository.remove(entityClass, entityId);
		if (isTransaction.get() && entity != null)
			saveEntityState(entityId, entity, EntityRecoveryState.ADD);
		return entity;
	}

	@Override
	public void load(Map<Class<?>, Map<Long, Object>> map) {
		repository.load(map);
	}

	@Override
	public <T> Optional<T> get(Class<T> entityType, long id) {
		Optional<T> entity = repository.get(entityType, id);
		if (entity == null)
			return Optional.empty();
		
		if (isTransaction.get() && entity.isPresent())
			saveEntityState(id, entity.get(), EntityRecoveryState.UPDATE);
		return entity;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Collection<T> getAll(Class<T> entityType) {
		return (Collection<T>) getEntities(entityType).values();
	}

	@Override
	public Map<Class<?>, Map<Long, Object>> getAll() {
		// TODO ?
		return repository.getAll();
	}

	@Override
	public Map<Long, Object> getEntities(Class<?> entityType) {
		Map<Long, Object> entities = repository.getEntities(entityType);
		if (isTransaction.get()) 
			entities.forEach((id, e) -> saveEntityState(id, e, EntityRecoveryState.UPDATE));
		return entities;
	}

	@Override
	public void begin() {
		isTransaction.set(true);
	}

	@Override
	public void commit() {
		isTransaction.set(false);
		clearResources();
	}

	@Override
	public void rollback() {
		restoreEntities().forEach(se -> {
			switch (states.get(se.getEntity().getClass()).get(se.getEntityId())) {
			case ADD: case UPDATE: repository.store(se.getEntityId(), se.getEntity()); break;
			case REMOVE: repository.remove(se.getEntity().getClass(), se.getEntityId()); break;
			}
		});
		clearResources();
	}
	
	protected boolean saveEntityState(long entityId, Object entity, EntityRecoveryState state) {
		if (!states.get(entity.getClass()).containsKey(entityId)) {
			marshallEntity(new SavedEntity(entity, entityId));
			fixedEntities.get(entity.getClass()).add(entityId);
			states.get(entity.getClass()).put(entityId, state);
			return true;
		} else
			return false;
	}
	
	protected List<SavedEntity> restoreEntities() {
		List<SavedEntity> result = new ArrayList<>();
		bufferMapping.forEach((type, hs) -> { hs.forEach(h -> {
			SavedEntity msg = schema.newMessage();
			try {
				schema.mergeFrom(h, msg);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			result.add(msg);
			h.clear();
		});
			hs.clear();
		});
		bufferMapping.clear();
		return result;
	}
	
	protected void marshallEntity(SavedEntity entity) {
		InputOutputHolder holder = new InputOutputHolder();
		try {
			schema.writeTo(holder, entity);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		bufferMapping.get(entity.getClass()).add(holder);
	}
	
	protected void clearResources() {
		bufferMapping.values().forEach(List::clear);
		bufferMapping.clear();
		fixedEntities.clear();
		states.clear();
	}
	
	public MutableModelRepository(WriteableRepository repository) {
		this.repository = repository;
	}
	
	protected final Map<Class<?>, List<InputOutputHolder>> bufferMapping = MapUtils.repositoryList();
	protected final Map<Class<?>, Set<Long>> fixedEntities = MapUtils.repositorySet();
	protected final Map<Class<?>, Map<Long, EntityRecoveryState>> states = MapUtils.repositoryMap();
	protected final WriteableRepository repository;
	protected final ThreadLocal<Boolean> isTransaction = new ThreadLocal<Boolean>() {
		protected Boolean initialValue() {
			return false;
		}
	};
	protected static final Schema<SavedEntity> schema = RuntimeSchema.getSchema(SavedEntity.class);
	
	public static enum EntityRecoveryState {
		ADD, REMOVE, UPDATE
	}
	
	public static class SavedEntity {
		public SavedEntity(Object entity, long entityId) {
			this.entity = entity;
			this.entityId = entityId;
		}
		
		private Object entity;
		public Object getEntity() {
			return entity;
		}
		
		private long entityId;
		public long getEntityId() {
			return entityId;
		}
		
		@Override
		public int hashCode() {
			int result = 1;
			result = 31 * result + (int) (entityId ^ (entityId >>> 32));
			result = 31 * result + entity.hashCode();
			return result;
		}
		
	}
	
}
