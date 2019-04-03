package org.reveno.atp.api.dynamic;

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class AbstractDynamicTransaction {
	protected Object2LongOpenHashMap<Class<?>> ids = new Object2LongOpenHashMap<>();
	protected Map<String, Object> args = new LinkedHashMap<>();

	public AbstractDynamicTransaction() {
	}

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

	public byte byteArg(String name) {
		return (byte) args.get(name);
	}

	public byte byteArg() {
		return (byte) arg();
	}

	public short shortArg(String name) {
		return (short) args.get(name);
	}

	public short shortArg() {
		return (short) arg();
	}

	public float floatArg(String name) {
		return (float) args.get(name);
	}

	public float floatArg() {
		return (float) arg();
	}

	public double doubleArg(String name) {
		return (double) args.get(name);
	}

	public double doubleArg() {
		return (double) arg();
	}

	public char charArg(String name) {
		return (char) args.get(name);
	}

	public char charArg() {
		return (char) arg();
	}

	public long longArg(String name) {
		return (long) args.get(name);
	}

	public long longArg() {
		return (long) arg();
	}

	public int intArg(String name) {
		return (int) args.get(name);
	}

	public int intArg() {
		return (int) arg();
	}

	public boolean bool(String name) {
		return (boolean) args.get(name);
	}

	public boolean bool() {
		return (boolean) arg();
	}
	
	public long id() {
		return ids.values().iterator().nextLong();
	}
	
	public long id(Class<?> type) {
		return ids.getLong(type);
	}
	
}
