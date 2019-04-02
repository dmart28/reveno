package org.reveno.atp.core.views;

import org.reveno.atp.api.query.ViewsMapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ViewsManager {
	protected Map<Class<?>, ViewHandlerHolder<?, ?>> viewsHandlers = new ConcurrentHashMap<>();
	protected Map<Class<?>, Class<?>> viewsToEntities = new ConcurrentHashMap<>();

	public <E, V> void register(Class<E> entityType, Class<V> viewType, ViewsMapper<E, V> mapper) {
		viewsHandlers.put(entityType, new ViewHandlerHolder<E, V>(viewType, mapper));
		viewsToEntities.put(viewType, entityType);
	}
	
	public boolean hasEntityMap(Class<?> entityType) {
		return viewsHandlers.containsKey(entityType);
	}
	
	public ViewHandlerHolder<?, ?> resolveEntity(Class<?> entityType) {
		return viewsHandlers.get(entityType);
	}
	
	public Class<?> resolveEntityType(Class<?> viewType) {
		return viewsToEntities.get(viewType);
	}

	
	public static class ViewHandlerHolder<E, V> {
		public Class<V> viewType;
		public ViewsMapper<E, V> mapper;
		
		public ViewHandlerHolder(Class<V> viewType, ViewsMapper<E, V> mapper) {
			this.viewType = viewType;
			this.mapper = mapper;
		}
	}
	
}
