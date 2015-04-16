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

import org.reveno.atp.api.domain.WriteableRepository;
import org.reveno.atp.api.query.QueryManager;
import org.reveno.atp.core.api.ViewsStorage;

public class ViewsDefaultStorage implements ViewsStorage, QueryManager  {

	@Override
	public <V> V find(Class<V> viewType, Long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <V> Collection<V> select(Class<V> viewType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <V> Collection<V> select(Class<V> viewType, Predicate<V> filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <V> Collection<V> parallelSelect(Class<V> viewType,
			Predicate<V> filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <View> Optional<View> find(Class<View> viewType, long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <View> void insert(long id, View view) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public <View> void remove(Class<View> viewType, long id) {
		// TODO Auto-generated method stub
		
	}
	
	public ViewsDefaultStorage(WriteableRepository repository) {
		this.repository = repository;
	}
	
	private WriteableRepository repository;
	
}
