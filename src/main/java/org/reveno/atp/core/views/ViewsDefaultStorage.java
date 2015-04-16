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
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.reveno.atp.api.domain.WriteableRepository;
import org.reveno.atp.api.query.QueryManager;
import org.reveno.atp.core.api.ViewsStorage;

public class ViewsDefaultStorage implements ViewsStorage, QueryManager  {

	@Override
	public <V> Collection<V> select(Class<V> viewType) {
		return repository.getAll(viewType);
	}

	@Override
	public <V> Collection<V> select(Class<V> viewType, Predicate<V> filter) {
		return repository.getAll(viewType).stream().filter(filter).collect(Collectors.toList());
	}

	@Override
	public <V> Collection<V> parallelSelect(Class<V> viewType, Predicate<V> filter) {
		return repository.getAll(viewType).stream().parallel().filter(filter).collect(Collectors.toList());
	}

	@Override
	public <View> Optional<View> find(Class<View> viewType, long id) {
		return Optional.ofNullable(repository.get(viewType, id));
	}

	@Override
	public <View> void insert(long id, View view) {
		repository.store(id, view);
	}

	@Override
	public <View> void remove(Class<View> viewType, long id) {
		repository.remove(viewType, id);
	}
	
	public ViewsDefaultStorage(WriteableRepository repository) {
		this.repository = repository;
	}
	
	private WriteableRepository repository;
	
}
