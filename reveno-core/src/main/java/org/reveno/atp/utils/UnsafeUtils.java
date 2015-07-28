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

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

public abstract class UnsafeUtils {

	public static Unsafe getUnsafe() {
		return unsafe;
	}

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

	public static void destroyDirectBuffer(ByteBuffer toBeDestroyed) {
		if (!toBeDestroyed.isDirect())
            return;

		Method cleanerMethod;
		try {
			cleanerMethod = toBeDestroyed.getClass().getMethod("cleaner");

			cleanerMethod.setAccessible(true);
			Object cleaner = cleanerMethod.invoke(toBeDestroyed);
			Method cleanMethod = cleaner.getClass().getMethod("clean");
			cleanMethod.setAccessible(true);
			cleanMethod.invoke(cleaner);
		} catch (NoSuchMethodException | SecurityException
				| IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new RuntimeException(e);
		}

	}
}
