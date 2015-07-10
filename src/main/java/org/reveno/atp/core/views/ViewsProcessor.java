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
import java.util.Set;

import org.reveno.atp.api.domain.Repository;
import org.reveno.atp.core.api.ViewsStorage;
import org.reveno.atp.core.engine.components.RecordingRepository;
import org.reveno.atp.core.views.ViewsManager.ViewHandlerHolder;

public class ViewsProcessor {
	
	protected static final int GET_ALL_SIZE = RecordingRepository.GET_ALL.size();

	public void process(Map<Class<?>, Set<Long>> marked) {
		marked.forEach((k, v) -> {
			if (v.size() >= GET_ALL_SIZE && v.containsAll(RecordingRepository.GET_ALL)) {
				repository.getEntities(k).forEach((id,e) -> map(k, id));
			} else {
				v.forEach(id -> map(k, id));
			}
		});
	}
	
	@SuppressWarnings("unchecked")
	protected void map(Class<?> entityType, long id) {
		ViewHandlerHolder<Object, Object> holder = (ViewHandlerHolder<Object, Object>) manager.resolveEntity(entityType);
		if (holder == null) 
			return;
		
		if (id >= 0) {
			Object view = holder.mapper.map((Object) repository.get(entityType, id).get(), 
					(Optional<Object>) storage.find(holder.viewType, id), repository);
			storage.insert(id, view);
		} else {
			storage.remove(holder.viewType, Math.abs(id));
		}
	}
	
	public ViewsProcessor(ViewsManager manager, ViewsStorage storage, Repository repository) {
		this.manager = manager;
		this.storage = storage;
		this.repository = repository;
	}
	
	protected ViewsManager manager;
	protected ViewsStorage storage;
	protected Repository repository;
}
