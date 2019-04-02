package org.reveno.atp.api.domain;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface Repository {
	
	/**
	 * Retrieves entity from repository. If current model is Mutable, then
	 * this entity also marked as dirty, which means that after current transaction execution
	 * view mapping for that entity will happen.
	 * 
	 * @param entityType entity type to be retrieved
	 * @param id identificator of an entity
	 * @return entity
	 */
	<T> T get(Class<T> entityType, long id);

	default <T> Optional<T> getO(Class<T> entityType, long id) {
		return Optional.ofNullable(get(entityType, id));
	}
	
	<T> boolean has(Class<T> entityType, long id);

	default <T> T getOrDefault(Class<T> entityType, long id, T defaultValue) {
		T t;
		return ((t = get(entityType, id)) != null) ? t : defaultValue;
	}
	
	/**
	 * Gets entity from repository without marking it as dirty, 
	 * hence no view mapping will happen after transaction, in which this method is invoked.
	 * 
	 * Please note, that above logic is actual only for Mutable Models.
	 * 
	 * @param entityType entity type to be retrieved
	 * @param id identificator of an entity
	 * @return entity
	 */
	<T> T getClean(Class<T> entityType, long id);
	
	/**
	 * Snapshot all data from the repository.
	 * 
	 * @return
	 */
	RepositoryData getData();
	
	Map<Long, Object> getEntities(Class<?> entityType);
	
	Map<Long, Object> getEntitiesClean(Class<?> entityType);
	
	/**
	 * All entity types registered in that repository.
	 * 
	 * @return
	 */
	Set<Class<?>> getEntityTypes();
	
}
