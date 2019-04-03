package org.reveno.atp.core.repository;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.reveno.atp.api.domain.RepositoryData;
import org.reveno.atp.api.domain.WriteableRepository;
import org.reveno.atp.utils.MapUtils;

import java.util.Map;
import java.util.Set;

@SuppressWarnings("unchecked")
public class HashMapRepository implements WriteableRepository {
    protected Map<Class<?>, Long2ObjectOpenHashMap<Object>> map;
    protected int capacity;
    protected float loadFactor;

    public HashMapRepository(int capacity, float loadFactor) {
        this.map = MapUtils.fastRepo(capacity, loadFactor);
        this.capacity = capacity;
        this.loadFactor = loadFactor;
    }

    @Override
    public <T> T get(Class<T> entityType, long id) {
        return (T) map.get(entityType).get(id);
    }

    @Override
    public <T> boolean has(Class<T> entityType, long id) {
        return get(entityType, id) != null;
    }

    @Override
    public <T> T getClean(Class<T> entityType, long id) {
        return get(entityType, id);
    }

    @Override
    public RepositoryData getData() {
        Map<Class<?>, Map<Long, Object>> data = MapUtils.repositoryMap();
        map.forEach((k, v) -> data.get(k).putAll(v));
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
    public <T> T remove(Class<T> entityClass, long entityId) {
        Long2ObjectOpenHashMap<Object> data = map.get(entityClass);
        if (data != null)
            return (T) data.remove(entityId);
        return null;
    }

    @Override
    public void load(Map<Class<?>, Map<Long, Object>> data) {
        data.forEach((k, v) -> map.put(k, new Long2ObjectOpenHashMap<>(v)));
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

}
