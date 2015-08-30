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

package org.reveno.atp.api.dynamic;

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AbstractDynamicTransaction {
	
	public Optional<Object> opArg(String name) {
		return Optional.ofNullable(args.get(name));
	}
	
	@SuppressWarnings("unchecked")
	public <T> T arg() {
		return (T) args.values().iterator().next();
	}
	
	@SuppressWarnings("unchecked")
	public <T> T arg(String name) {
		return (T) args.get(name);
	}
	
	public long id() {
		return ids.values().iterator().nextLong();
	}
	
	public long id(Class<?> type) {
		return ids.getLong(type);
	}
	
	public AbstractDynamicTransaction() {
	}
	
	protected Object2LongOpenHashMap<Class<?>> ids = new Object2LongOpenHashMap<Class<?>>();
	protected Map<String, Object> args = new HashMap<>();
	
}
