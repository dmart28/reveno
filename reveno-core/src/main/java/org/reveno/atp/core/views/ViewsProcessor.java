package org.reveno.atp.core.views;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.reveno.atp.api.domain.Repository;
import org.reveno.atp.core.api.ViewsStorage;
import org.reveno.atp.core.views.ViewsManager.ViewHandlerHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.Consumer;

@SuppressWarnings("all")
public class ViewsProcessor {
	private static final Logger LOG = LoggerFactory.getLogger(ViewsProcessor.class);
	protected ViewsManager manager;
	protected ViewsStorage storage;
	protected OnDemandViewsContext repository;

	public ViewsProcessor(ViewsManager manager, ViewsStorage storage) {
		this.manager = manager;
		this.storage = storage;
		this.repository = new OnDemandViewsContext(this, storage, manager);
	}

	public void process(Repository repo) {
		repository.repositorySource(repo);
		repo.getEntityTypes().forEach(c -> repo.getEntities(c).forEach((k,v) -> {
			try {
				map(c, k, v);
			} catch (Throwable t) {
				LOG.error(t.getMessage(), t);
			}
		}));
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
}
