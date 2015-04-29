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

package org.reveno.atp.acceptance.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class Immutable {
	
	@SuppressWarnings("unchecked")
	public static <T, U> HashMap<T, U> ma(HashMap<T, U> map, T key, U value) {
		HashMap<T, U> c = (HashMap<T, U>)map.clone();
		c.put(key, value);
		return c;
	}
	
	@SuppressWarnings("unchecked")
	public static <T, U> HashMap<T, U> mr(HashMap<T, U> map, T key) {
		HashMap<T, U> c = (HashMap<T, U>)map.clone();
		c.remove(key);
		return c;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> HashSet<T> sa(HashSet<T> set, T value) {
		HashSet<T> c = (HashSet<T>)set.clone();
		c.add(value);
		return c;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> HashSet<T> sr(HashSet<T> set, T value) {
		HashSet<T> c = (HashSet<T>)set.clone();
		c.remove(value);
		return c;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> ArrayList<T> la(ArrayList<T> set, T value) {
		ArrayList<T> c = (ArrayList<T>)set.clone();
		c.add(value);
		return c;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> ArrayList<T> laa(ArrayList<T> set, Collection<T> value) {
		ArrayList<T> c = (ArrayList<T>)set.clone();
		c.addAll(value);
		return c;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> ArrayList<T> lr(ArrayList<T> set, T value) {
		ArrayList<T> c = (ArrayList<T>)set.clone();
		c.remove(value);
		return c;
	}
	
	public static Map<String, Object> pair(String str, Object o) {
		Map<String, Object> m = new HashMap<>();
		m.put(str, o);
		return m;
	}
	
	public static Map<String, Object> pair(String str, Object o, String str1, Object o1) {
		Map<String, Object> m = new HashMap<>();
		m.put(str, o);
		m.put(str1, o1);
		return m;
	}

	@SuppressWarnings("unchecked")
	public static <T> T copy(T entity, Map<String, Object> values) {
		registerIfRequired(entity.getClass());
		
		Constructor<T> t = (Constructor<T>) constructors.get(entity.getClass());
		
		List<Object> args = fields.get(entity.getClass()).entrySet().stream().map(e -> {
			if (values.containsKey(e.getKey()))
				return values.get(e.getKey());
			else
				try {
					return e.getValue().get(entity);
				} catch (Exception e1) {
					throw new RuntimeException(e1);
				}
		}).collect(Collectors.toList());
		
		try {
			return t.newInstance(args.toArray());
		} catch (InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException e1) {
			throw new RuntimeException("" + entity.getClass() + "\n" + t.getParameterCount(), e1);
		}
	}
	
	
	protected static void registerIfRequired(Class<?> type) {
		if (!fields.containsKey(type)) {
			fields.put(type, new LinkedHashMap<>());
			for (Field f : type.getDeclaredFields()) {
				f.setAccessible(true);
				
				if (Modifier.isFinal(f.getModifiers()) && !Modifier.isStatic(f.getModifiers())) {
					fields.get(type).put(f.getName(), f);
				}
			}
			
			Constructor<?> c = Arrays.asList(type.getConstructors()).stream().max((c1, c2) -> {
				if (c1.getParameterCount() > c2.getParameterCount()) 
					return 1;
				else if (c1.getParameterCount() < c2.getParameterCount()) 
					return -1;
				else 
					return 0;
			}).get();
			constructors.put(type, c);
		}
	}
	
	protected static final Map<Class<?>, Map<String, Field>> fields = new HashMap<>();
	protected static final Map<Class<?>, Constructor<?>> constructors = new HashMap<>();
}
