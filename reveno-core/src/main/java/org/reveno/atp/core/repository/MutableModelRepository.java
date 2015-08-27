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

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.reveno.atp.api.domain.RepositoryData;
import org.reveno.atp.api.domain.WriteableRepository;
import org.reveno.atp.core.api.Destroyable;
import org.reveno.atp.core.api.TxRepository;
import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.api.serialization.Serializer;
import org.reveno.atp.core.channel.ChannelBuffer;
import org.reveno.atp.utils.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.reveno.atp.utils.MeasureUtils.kb;

public class MutableModelRepository implements TxRepository, Destroyable {

	@Override
	public <T> T store(long entityId, T entity) {
		repository.store(entityId, entity);
		if (isTransaction.get() && entity != null)
			saveEntityState(entityId, entity.getClass(), entity, EntityRecoveryState.REMOVE);
		return entity;
	}
	
	@Override
	public <T> T store(long entityId, Class<? super T> type, T entity) {
		repository.store(entityId, type, entity);
		if (isTransaction.get() && entity != null)
			saveEntityState(entityId, type, entity, EntityRecoveryState.REMOVE);
		return entity;
	}

	@Override
	public Object remove(Class<?> entityClass, long entityId) {
		Object entity = repository.remove(entityClass, entityId);
		if (isTransaction.get() && entity != null)
			saveEntityState(entityId, entityClass, entity, EntityRecoveryState.ADD);
		return entity;
	}

	@Override
	public void load(Map<Class<?>, Map<Long, Object>> map) {
		repository.load(map);
	}

	@Override
	public <T> Optional<T> get(Class<T> entityType, long id) {
		Optional<T> entity = repository.get(entityType, id);
		
		if (isTransaction.get() && entity.isPresent())
			saveEntityState(id, entityType, entity.get(), EntityRecoveryState.UPDATE);
		return entity;
	}
	
	@Override
	public <T> boolean has(Class<T> entityType, long id) {
		return getClean(entityType, id).isPresent();
	}

	@Override
	public RepositoryData getData() {
		return repository.getData();
	}

	@Override
	public Map<Long, Object> getEntities(Class<?> entityType) {
		Map<Long, Object> entities = repository.getEntities(entityType);
		if (isTransaction.get())
			entities.forEach((id, e) -> saveEntityState(id, entityType, e, EntityRecoveryState.UPDATE));
		return entities;
	}
	
	@Override
	public <T> Optional<T> getClean(Class<T> entityType, long id) {
		return repository.get(entityType, id);
	}

	@Override
	public Map<Long, Object> getEntitiesClean(Class<?> entityType) {
		return repository.getEntities(entityType);
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

	@SuppressWarnings("unchecked")
	@Override
	public void rollback() {
		try {
			buffer.setReaderPosition(0);
			for (int i = 0; i < stashedObjects; i++) {
				EntityRecoveryState state = EntityRecoveryState.getByType(buffer.readByte());
				long entityId = buffer.readLong();
				Class<?> type = null;
				if (buffer.readByte() == 1) {
					byte[] classNameBin = new byte[buffer.readInt()];
					buffer.readBytes(classNameBin, 0, classNameBin.length);
					try {
						type = classLoader.loadClass(new String(classNameBin));
					} catch (ClassNotFoundException e) {
						log.error(e.getMessage(), e);
					}
				}
				Object entity = serializer.deserializeObject(buffer);
				if (type == null)
					type = entity.getClass();

				switch (state) {
				case ADD:
				case UPDATE:repository.store(entityId, (Class<Object>) type, entity);break;
				case REMOVE:repository.remove(type, entityId);break;
				}
			}
		} finally {
			clearResources();
		}
	}
	
	@Override
	public Set<Class<?>> getEntityTypes() {
		return repository.getEntityTypes();
	}
	
	@Override
	public void destroy() {
		buffer.release();
	}
	
	protected boolean saveEntityState(long entityId, Class<?> type, Object entity, EntityRecoveryState state) {
		if (!stashed.get(type).contains(entityId)) {
			if (!serializer.isRegistered(entity.getClass()))
				serializer.registerTransactionType(entity.getClass());
			marshallEntity(entityId, type, entity, state);
			stashed.get(type).add(entityId);
			return true;
		} else
			return false;
	}
	
	protected void marshallEntity(long entityId, Class<?> type, Object entity, EntityRecoveryState state) {
		stashedObjects++;
		
		buffer.writeByte(state.getType());
		buffer.writeLong(entityId);
		if (!entity.getClass().equals(type)) {
			buffer.writeByte((byte)1);
			buffer.writeInt(type.getName().length());
			buffer.writeBytes(type.getName().getBytes(Charset.forName("ISO-8859-1")));
		} else {
			buffer.writeByte((byte)0);
		}
		serializer.serializeObject(buffer, entity);
	}
	
	protected void clearResources() {
		stashedObjects = 0;
		buffer.clear();
		stashed.forEach((k,v) -> v.clear());
	}
	
	public MutableModelRepository(WriteableRepository repository, Serializer serializer) {
		this(repository, serializer, MutableModelRepository.class.getClassLoader());
	}
	
	public MutableModelRepository(WriteableRepository repository, Serializer serializer, ClassLoader classLoader) {
		this.repository = repository;
		this.serializer = serializer;
		this.classLoader = classLoader;
	}
	
	protected int stashedObjects = 0;
	protected final Map<Class<?>, LongOpenHashSet> stashed = MapUtils.fastSetRepo();
	protected final WriteableRepository repository;
	protected final Serializer serializer;
	protected final ClassLoader classLoader;
	protected final Buffer buffer = new ChannelBuffer(ByteBuffer.allocateDirect(kb(128)));
	protected final ThreadLocal<Boolean> isTransaction = new ThreadLocal<Boolean>() {
		protected Boolean initialValue() {
			return false;
		}
	};
	protected static final Logger log = LoggerFactory.getLogger(MutableModelRepository.class);
	
	public static enum EntityRecoveryState {
		ADD((byte)1), REMOVE((byte)2), UPDATE((byte)3);
		
		protected byte type;
		public byte getType() {
			return type;
		}
		public static EntityRecoveryState getByType(byte type) {
			for (EntityRecoveryState s : values())
				if (s.getType() == type)
					return s;
			throw new IllegalArgumentException(String.format("Can't find Entity Recovery type %d", type));
		}
		
		EntityRecoveryState(byte type) {
			this.type = type;
		}
	}
	
}
