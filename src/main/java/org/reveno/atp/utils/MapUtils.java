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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

public abstract class MapUtils {
	
	public static SimpleMap<Class<?>, Long2ObjectLinkedOpenHashMap<Object>> linkedFastRepo() {
		return new SimpleMap<>(() -> new Long2ObjectLinkedOpenHashMap<>());
	}
	
	public static SimpleMap<Class<?>, Long2ObjectOpenHashMap<Object>> fastRepo() {
		return new SimpleMap<>(() -> new Long2ObjectOpenHashMap<>());
	}
	
	public static SimpleMap<Class<?>, Long2ObjectOpenHashMap<Object>> fastRepo(int capacity, float loadFactor) {
		return new SimpleMap<>(capacity, loadFactor, () -> new Long2ObjectOpenHashMap<>(capacity, loadFactor));
	}

	public static <T> ConcurrentMapOfMap<Class<?>, Long, T> concurrentRepositoryMap() {
		return new ConcurrentMapOfMap<Class<?>, Long, T>();
	}
	
	public static <T> ConcurrentMapOfMap<Class<?>, Long, T> concurrentRepositoryMap(int capacity, float loadFactor) {
		return new ConcurrentMapOfMap<Class<?>, Long, T>(capacity, loadFactor);
	}
	
	public static <T> MapOfMap<Class<?>, Long, T> repositoryMap() {
		return new MapOfMap<Class<?>, Long, T>();
	}
	
	public static <T> MapOfList<Class<?>, T> repositoryList() {
		return new MapOfList<Class<?>, T>();
	}
	
	public static <T> MapOfSet<Class<?>, T> repositorySet() {
		return new MapOfSet<Class<?>, T>();
	}
	
	public static SimpleMap<Class<?>, LongOpenHashSet> fastSetRepo() {
		return new SimpleMap<Class<?>, LongOpenHashSet>(() -> new LongOpenHashSet());
	}
	
	public static <T> MapOfSet<Class<?>, Long> repositoryLinkedSet() {
		return new MapOfSet<Class<?>, Long>(() -> new LongLinkedOpenHashSet(), new LinkedHashMap<>());
	}
	
	public static class SimpleMap<K, V> extends HashMap<K, V> implements Map<K, V> {
		private static final long serialVersionUID = 1L;
		private final Supplier<V> supplier;
		
		public SimpleMap(int capacity, float loadFactor, Supplier<V> supplier) {
			super(capacity, loadFactor);
			this.supplier = supplier;
		}
		
		public SimpleMap(Supplier<V> supplier) {
			this.supplier = supplier;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public V get(Object key) {
			return safeGet((K) key);
		}
		
		public V safeGet(K key) {
			V result = super.get(key);
			if (result == null)
				this.put(key, (result = supplier.get()));
			return result;
		}
	}
	
	public static class MapOfMap<T, U, M> extends HashMap<T, Map<U, M>> implements Map<T, Map<U, M>> {
		private static final long serialVersionUID = 8689714774124849342L;
		
		@SuppressWarnings("unchecked")
		@Override
		public Map<U, M> get(Object key) {
			return safeGet((T) key);
		}
		
		public Map<U, M> safeGet(T key) {
			Map<U, M> result = super.get(key);
			if (result == null)
				this.put(key, (result = new HashMap<>()));
			return result;
		}
	}
	
	public static class ConcurrentMapOfMap<T, U, M> extends ConcurrentHashMap<T, Map<U, M>> implements Map<T, Map<U, M>> {
		private static final long serialVersionUID = 8689714774124849342L;
		
		public ConcurrentMapOfMap() {
		}
		
		public ConcurrentMapOfMap(int capacity, float loadFactor) {
			super(capacity, loadFactor);
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public Map<U, M> get(Object key) {
			return safeGet((T) key);
		}
		
		public Map<U, M> safeGet(T key) {
			Map<U, M> result = super.get(key);
			if (result == null)
				this.put(key, (result = new ConcurrentHashMap<>()));
			return result;
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
			List<U> result = super.get(key);
			if (result == null)
				this.put(key, (result = new ArrayList<>()));
			return result;
		}
	}
	
	public static class MapOfSet<T, U> implements Map<T, Set<U>> {
		protected final Supplier<Set<U>> setCreator;
		protected final Map<T, Set<U>> underlying;
		
		@SuppressWarnings("unchecked")
		@Override
		public Set<U> get(Object key) {
			return safeGet((T) key);
		}
		
		public Set<U> safeGet(T key) {
			Set<U> result = underlying.get(key);
			if (result == null) 
				this.put(key, (result = setCreator.get()));
			return result;
		}
		
		public MapOfSet() {
			this.setCreator = () -> new HashSet<U>();
			this.underlying = new HashMap<>();
		}
		
		public MapOfSet(Supplier<Set<U>> setCreator, Map<T, Set<U>> underlying) {
			this.setCreator = setCreator;
			this.underlying = underlying;
		}

		@Override
		public int size() {
			return underlying.size();
		}

		@Override
		public boolean isEmpty() {
			return underlying.isEmpty();
		}

		@Override
		public boolean containsKey(Object key) {
			return underlying.containsKey(key);
		}

		@Override
		public boolean containsValue(Object value) {
			return underlying.containsValue(value);
		}

		@Override
		public Set<U> put(T key, Set<U> value) {
			return underlying.put(key, value);
		}

		@Override
		public Set<U> remove(Object key) {
			return underlying.remove(key);
		}

		@Override
		public void putAll(Map<? extends T, ? extends Set<U>> m) {
			this.putAll(m);
		}

		@Override
		public void clear() {
			underlying.clear();
		}

		@Override
		public Set<T> keySet() {
			return underlying.keySet();
		}

		@Override
		public Collection<Set<U>> values() {
			return underlying.values();
		}

		@Override
		public Set<java.util.Map.Entry<T, Set<U>>> entrySet() {
			return underlying.entrySet();
		}
	}
	
}
