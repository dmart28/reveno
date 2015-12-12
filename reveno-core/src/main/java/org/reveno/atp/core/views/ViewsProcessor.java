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

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.reveno.atp.api.domain.Repository;
import org.reveno.atp.core.api.ViewsStorage;
import org.reveno.atp.core.views.ViewsManager.ViewHandlerHolder;

import java.util.Map;
import java.util.function.Consumer;

@SuppressWarnings("all")
public class ViewsProcessor {
	protected ViewsManager manager;
	protected ViewsStorage storage;
	protected OnDemandViewsContext repository;

	public void process(Repository repo) {
		repository.repositorySource(repo);
		repo.getEntityTypes().forEach(c -> repo.getEntities(c).forEach((k,v) -> map(c, k, v)));
		repository.repositorySource(null);
	}

	public ViewHandlerHolder<Object, Object> currentHandler;
	private final Consumer<? super Long2ObjectMap.Entry<Object>> m = new Consumer<Long2ObjectMap.Entry<Object>>() {
		@Override
		public void accept(Long2ObjectMap.Entry<Object> entry) {
			map(currentHandler, entry.getLongKey(), entry.getValue());
		}
	};
	private final Consumer<Map.Entry<Class<?>, Long2ObjectLinkedOpenHashMap<Object>>> c = e -> {
		ViewHandlerHolder<Object, Object> holder = (ViewHandlerHolder<Object, Object>) manager.resolveEntity(e.getKey());
		if (holder != null) {
			currentHandler = holder;
			e.getValue().long2ObjectEntrySet().forEach(m);
		}
	};

	public void process(Map<Class<?>, Long2ObjectLinkedOpenHashMap<Object>> marked) {
		repository.marked(marked);
		marked.entrySet().forEach(c);
	}

	public void erase() {
		storage.clearAll();
	}

	protected void map(Class<?> type, long id, Object entity) {
		ViewHandlerHolder<Object, Object> holder = (ViewHandlerHolder<Object, Object>) manager.resolveEntity(type);
		if (holder != null) {
			map(holder, id, entity);
		}
	}

	protected void map(ViewHandlerHolder<Object, Object> holder, long id, Object entity) {
		if (id >= 0) {
			repository.currentId(id);
			repository.currentViewType(holder.viewType);
			
			Object view = holder.mapper.map(id, entity, repository);
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
	
}
