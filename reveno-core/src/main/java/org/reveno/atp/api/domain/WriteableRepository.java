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

package org.reveno.atp.api.domain;

import java.util.Map;

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
