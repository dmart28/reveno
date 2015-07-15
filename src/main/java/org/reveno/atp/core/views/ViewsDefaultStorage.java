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

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.reveno.atp.api.query.QueryManager;
import org.reveno.atp.core.api.ViewsStorage;
import org.reveno.atp.utils.MapUtils;

/**
 * Not efficient version, consider for changing it.
 * 
 * @author Artem Dmitriev <art.dm.ser@gmail.com>
 *
 */
public class ViewsDefaultStorage implements ViewsStorage, QueryManager  {

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
	public <View> Optional<View> find(Class<View> viewType, long id) {
		return Optional.ofNullable((View)views.get(viewType).get(id));
	}

	@Override
	public <View> void insert(long id, View view) {
		views.get(view.getClass()).put(id, view);
	}

	@Override
	public <View> void remove(Class<View> viewType, long id) {
		views.get(viewType).remove(id);
	}
	
	public ViewsDefaultStorage() {
		views = MapUtils.concurrentRepositoryMap(524288, 0.75f);
	}
	
	public ViewsDefaultStorage(int capacity, float loadFactor) {
		views = MapUtils.concurrentRepositoryMap(capacity, loadFactor);
	}
	
	private final Map<Class<?>, Map<Long, Object>> views;
	
}
