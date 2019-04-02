package org.reveno.atp.utils;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@SuppressWarnings({ "unchecked", "rawtypes" })
public abstract class MapUtils {
	
	public static Map<String, Object> map(Object... objs) {
		if (objs.length % 2 != 0) {
			throw new IllegalArgumentException("Input map should contain even count of arguments.");
		}
		
		Map<String, Object> map = new LinkedHashMap<>();
		
		for (int i = 0; i < objs.length; i += 2) {
			map.put((String) objs[i], objs[i + 1]);
		}
		
		return map;
	}
	
	public static SimpleMap<Class<?>, Long2ObjectLinkedOpenHashMap<Object>> linkedFastRepo() {
		return new SimpleMap<Class<?>, Long2ObjectLinkedOpenHashMap<Object>>((Supplier & Serializable)Long2ObjectLinkedOpenHashMap::new);
	}
	
	public static SimpleMap<Class<?>, Long2ObjectOpenHashMap<Object>> fastRepo() {
		return new SimpleMap<Class<?>, Long2ObjectOpenHashMap<Object>>((Supplier & Serializable)Long2ObjectOpenHashMap::new);
	}
	
	public static SimpleMap<Class<?>, Long2ObjectOpenHashMap<Object>> fastRepo(int capacity, float loadFactor) {
		return new SimpleMap<Class<?>, Long2ObjectOpenHashMap<Object>>(capacity, loadFactor,
				(Supplier & Serializable)() -> new Long2ObjectOpenHashMap<>(capacity, loadFactor));
	}

	public static <T> ConcurrentMapOfMap<Class<?>, Long, T> concurrentRepositoryMap() {
		return new ConcurrentMapOfMap<>();
	}
	
	public static <T> ConcurrentMapOfMap<Class<?>, Long, T> concurrentRepositoryMap(int capacity, float loadFactor) {
		return new ConcurrentMapOfMap<>(capacity, loadFactor);
	}
	
	public static <T> MapOfMap<Class<?>, Long, T> repositoryMap() {
		return new MapOfMap<>();
	}
	
	public static <T> MapOfList<Class<?>, T> repositoryList() {
		return new MapOfList<>();
	}
	
	public static <T> MapOfSet<Class<?>, T> repositorySet() {
		return new MapOfSet<>();
	}
	
	public static SimpleMap<Class<?>, LongOpenHashSet> fastSetRepo() {
		return new SimpleMap<>(LongOpenHashSet::new);
	}
	
	public static <T> MapOfSet<Class<?>, Long> repositoryLinkedSet() {
		return new MapOfSet<>(LongLinkedOpenHashSet::new, new LinkedHashMap<>());
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
		
		@Override
		public Map<U, M> get(Object key) {
			return safeGet((T) key);
		}
		
		public Map<U, M> safeGet(T key) {
			return super.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
		}
	}
	
	public static class MapOfList<T, U> extends HashMap<T, List<U>> implements Map<T, List<U>> {
		private static final long serialVersionUID = -5096309465438907445L;

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
			this.setCreator = HashSet::new;
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
