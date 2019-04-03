package org.reveno.atp.utils;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

public abstract class UnsafeUtils {
	private static final Unsafe unsafe;

	static {
		try {
			Field field = Unsafe.class.getDeclaredField("theUnsafe");
			field.setAccessible(true);
			unsafe = (Unsafe) field.get(null);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static Unsafe getUnsafe() {
		return unsafe;
	}

	public static void destroyDirectBuffer(ByteBuffer toBeDestroyed) {
		if (!toBeDestroyed.isDirect()) {
			return;
		}

		Method cleanerMethod;
		try {
			cleanerMethod = toBeDestroyed.getClass().getMethod("cleaner");

			cleanerMethod.setAccessible(true);
			Object cleaner = cleanerMethod.invoke(toBeDestroyed);
			if (cleaner != null) {
				Method cleanMethod = cleaner.getClass().getMethod("clean");
				cleanMethod.setAccessible(true);
				cleanMethod.invoke(cleaner);
			}
		} catch (NoSuchMethodException | SecurityException
				| IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			//throw new RuntimeException(e);
		}

	}
}
