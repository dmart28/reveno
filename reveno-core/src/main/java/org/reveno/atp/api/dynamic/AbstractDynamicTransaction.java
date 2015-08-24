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
