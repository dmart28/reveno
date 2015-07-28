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
	<T> Optional<T> get(Class<T> entityType, long id);
	
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
	<T> Optional<T> getClean(Class<T> entityType, long id);
	
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
