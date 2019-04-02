package org.reveno.atp.core.views;

import org.reveno.atp.api.query.QueryManager;
import org.reveno.atp.core.api.ViewsStorage;
import org.reveno.atp.utils.MapUtils;

import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Not efficient version, consider for changing it.
 * 
 * @author Artem Dmitriev <art.dm.ser@gmail.com>
 *
 */
public class ViewsDefaultStorage implements ViewsStorage, QueryManager  {
	private final Map<Class<?>, Map<Long, Object>> views;

	public ViewsDefaultStorage(int capacity, float loadFactor) {
		views = MapUtils.concurrentRepositoryMap(capacity, loadFactor);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <V> Collection<V> select(Class<V> viewType) {
		return views.get(viewType).values().stream().map(i -> (V)i).collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	@Override
	public <V> Collection<V> select(Class<V> viewType, Predicate<V> filter) {
		return views.get(viewType).values().stream().map(i -> (V)i).filter(filter).collect(Collectors.toList());
	}

	@Override
	public <V> Collection<V> parallelSelect(Class<V> viewType, Predicate<V> filter) {
		return select(viewType).stream().parallel().filter(filter).collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	@Override
	public <V> V find(Class<V> viewType, long id) {
		return (V) views.get(viewType).get(id);
	}

	@Override
	public <View> void insert(long id, View view) {
		views.get(view.getClass()).put(id, view);
	}

	@Override
	public <View> void remove(Class<View> viewType, long id) {
		views.get(viewType).remove(id);
	}

	@Override
	public void clearAll() {
		views.clear();
	}
}
