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

package org.reveno.atp.core.views;

import java.util.Map;
import java.util.Optional;

import org.reveno.atp.api.domain.Repository;
import org.reveno.atp.core.api.ViewsStorage;
import org.reveno.atp.core.views.ViewsManager.ViewHandlerHolder;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

public class ViewsProcessor {

	public void process(Repository repo) {
		repository.repositorySource(repo);
		repo.getEntityTypes().forEach(c -> repo.getEntities(c).forEach((k,v) -> map(c, k, v)));
		repository.repositorySource(null);
	}
	
	public void process(Map<Class<?>, Long2ObjectLinkedOpenHashMap<Object>> marked) {
		repository.marked(marked);
		marked.forEach((k, v) -> {
			v.long2ObjectEntrySet().forEach(e -> {
				map(k, e.getLongKey(), e.getValue());
			});
		});
	}
	
	@SuppressWarnings("unchecked")
	protected void map(Class<?> entityType, long id, Object entity) {
		ViewHandlerHolder<Object, Object> holder = (ViewHandlerHolder<Object, Object>) manager.resolveEntity(entityType);
		if (holder == null) 
			return;
		
		if (id >= 0) {
			Object view = holder.mapper.map(entity, (Optional<Object>) storage.find(holder.viewType, id), repository);
			storage.insert(id, view);
		} else {
			storage.remove(holder.viewType, Math.abs(id));
		}
	}
	
	public ViewsProcessor(ViewsManager manager, ViewsStorage storage) {
		this.manager = manager;
		this.storage = storage;
		this.repository = new OnDemandViewsContext(this, storage, manager);
	}
	
	protected ViewsManager manager;
	protected ViewsStorage storage;
	protected OnDemandViewsContext repository;
	
}
