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

package org.reveno.atp.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class MapUtils {

	public static <T> MapOfMap<Class<?>, Long, T> repositoryMap() {
		return new MapOfMap<Class<?>, Long, T>();
	}
	
	public static <T> MapOfList<Class<?>, T> repositoryList() {
		return new MapOfList<Class<?>, T>();
	}
	
	public static <T> MapOfSet<Class<?>, T> repositorySet() {
		return new MapOfSet<Class<?>, T>();
	}
	
	public static class MapOfMap<T, U, M> extends HashMap<T, Map<U, M>> implements Map<T, Map<U, M>> {
		private static final long serialVersionUID = 8689714774124849342L;
		
		@SuppressWarnings("unchecked")
		@Override
		public Map<U, M> get(Object key) {
			return safeGet((T) key);
		}
		
		public Map<U, M> safeGet(T key) {
			if (!this.containsKey(key))
				this.put(key, new HashMap<>());
			return super.get(key);
		}
	}
	
	public static class MapOfList<T, U> extends HashMap<T, List<U>> implements Map<T, List<U>> {
		private static final long serialVersionUID = -5096309465438907445L;

		@SuppressWarnings("unchecked")
		@Override
		public List<U> get(Object key) {
			return safeGet((T) key);
		}
		
		public List<U> safeGet(T key) {
			if (!this.containsKey(key))
				this.put(key, new ArrayList<>());
			return super.get(key);
		}
	}
	
	public static class MapOfSet<T, U> extends HashMap<T, Set<U>> implements Map<T, Set<U>> {
		private static final long serialVersionUID = 1412151511553511511L;
		
		@SuppressWarnings("unchecked")
		@Override
		public Set<U> get(Object key) {
			return safeGet((T) key);
		}
		
		public Set<U> safeGet(T key) {
			if (!this.containsKey(key)) 
				this.put(key, new HashSet<>());
			return super.get(key);
		}
	}
	
}
