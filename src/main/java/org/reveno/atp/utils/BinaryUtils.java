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

public abstract class BinaryUtils {

	public static long bytesToLong(byte[] b) {
		return ((((long) b[0]) << 56) | (((long) b[1] & 0xff) << 48) | (((long) b[2] & 0xff) << 40)
	            | (((long) b[3] & 0xff) << 32) | (((long) b[4] & 0xff) << 24) | (((long) b[5] & 0xff) << 16)
	            | (((long) b[6] & 0xff) << 8) | (((long) b[7] & 0xff)));
	}
	
	public static byte[] longToBytes(long l) {
		return new byte[] {
		        (byte) (l >> 56),
		        (byte) (l >> 48),
		        (byte) (l >> 40),
		        (byte) (l >> 32),
		        (byte) (l >> 24),
		        (byte) (l >> 16),
		        (byte) (l >> 8),
		        (byte) l
		    };
	}
	
	public static int bytesToInt(byte[] b) {
		return ((((int) b[0] & 0xff) << 24) | (((int) b[1] & 0xff) << 16)
	            | (((int) b[2] & 0xff) << 8) | (((int) b[3] & 0xff)));
	}
	
	public static byte[] intToBytes(int l) {
		return new byte[] {
		        (byte) (l >>> 24),
		        (byte) (l >>> 16),
		        (byte) (l >>> 8),
		        (byte) l
		    };
	}
	
}
