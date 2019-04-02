package org.reveno.atp.api.domain;

import org.reveno.atp.commons.BiLongConsumer;
import org.reveno.atp.commons.BiLongFunction;

import java.util.Map;
import java.util.function.Supplier;

public interface WriteableRepository extends Repository {

	/**
	 * Stores new entity to the repository. It will be then available in repository
	 * under {@code entityId} and {@code entity.getClass()} values strictly.
	 *
	 * @param entityId unique identity of the entity being stored
	 * @param entity entity being stored
	 * @param <T>
     * @return old entity that was stored under this keys, {@code null} unless
     */
	<T> T store(long entityId, T entity);

	default <T> T storeIfAbsent(long entityId, T entity) {
		T t;
		if ((t = get((Class<T>) entity.getClass(), entityId)) == null)
			return store(entityId, entity);
		return t;
	}

	/**
	 * Stores new entity to the repository. You should explicitly define the Class instance
	 * under which it will be stored along with unique entity identity.
	 *
	 * @param entityId unique identity of the entity being stored
	 * @param type explicit type of entity
	 * @param entity entity being stored
	 * @param <T>
     * @return old entity that was stored under this keys, {@code null} unless
     */
	<T> T store(long entityId, Class<? super T> type, T entity);

	default <T> T storeIfAbsent(long entityId, Class<? super T> type, T entity) {
		T t;
		if ((t = get((Class<T>) type, entityId)) == null)
			return store(entityId, type, entity);
		return t;
	}

	/**
	 * If there is an existing entity with {@code entityId} and {@code type} in Repository,
	 * it will be replaced with the value returned by {@code remap} call, otherwise nothing will
	 * happen.
	 *
	 * @param entityId
	 * @param type
	 * @param remap
	 * @param <T>
     * @return previously stored value or null if there was no such entity
     */
	default <T> T remap(long entityId, Class<T> type, BiLongFunction<? super T, ? extends T> remap) {
		T t;
		if ((t = get(type, entityId)) != null) {
			return store(entityId, type, remap.apply(entityId, t));
		}
		return null;
	}

	/**
	 * Version of {@link #remap(long, Class, BiLongFunction)} function for immutable repository.
	 *
	 * @param entityId
	 * @param type
	 * @param remap
     * @param <T>
     */
	default <T> void remap(Class<T> type, long entityId, BiLongConsumer<? super T> remap) {
		T t;
		if ((t = get(type, entityId)) != null) {
			remap.accept(entityId, t);
		}
	}

	/**
	 * If the specified entity by such id is not exists in repository, {@code entity} would
	 * be stored, otherwise, non-null result of {@code remap} execution. If {@code remap} will
	 * yield {@code null}, entity located by this key will be removed from repository.
	 *
	 * @param entityId
	 * @param entity
	 * @param remap
	 * @param <T>
     * @return
     */
	default <T> T merge(long entityId, Class<T> type, Supplier<T> entity, BiLongFunction<? super T, ? extends T> remap) {
		T oldValue = getClean(type, entityId);
		T newValue = (oldValue == null) ? entity.get() : remap.apply(entityId, oldValue);
		if (newValue == null) {
			remove(type, entityId);
		} else {
			store(entityId, type, newValue);
		}
		return newValue;
	}

	/**
	 * Removes entity from repository.
	 *
	 * @param entityClass type of entity
	 * @param entityId identity of entity
	 * @param <T>
     * @return last stored value if any, {@code null} unless
     */
	<T> T remove(Class<T> entityClass, long entityId);
	
	void load(Map<Class<?>, Map<Long, Object>> map);
	
}
