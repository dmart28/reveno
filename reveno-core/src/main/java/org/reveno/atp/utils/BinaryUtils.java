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

import it.unimi.dsi.fastutil.bytes.ByteArrays;
import org.reveno.atp.core.api.channel.Buffer;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.CRC32;

public abstract class BinaryUtils {
	private static final MessageDigest SHA1;

	static {
		try {
			SHA1 = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException e) {
			throw Exceptions.runtime(e);
		}
	}

	public static long bytesToLong(byte[] b) {
		return ((((long) b[0]) << 56) | (((long) b[1] & 0xff) << 48) | (((long) b[2] & 0xff) << 40)
	            | (((long) b[3] & 0xff) << 32) | (((long) b[4] & 0xff) << 24) | (((long) b[5] & 0xff) << 16)
	            | (((long) b[6] & 0xff) << 8) | (((long) b[7] & 0xff)));
	}
	
	public static void longToBytes(long l, byte[] b) {
		b[0] = (byte) (l >> 56);
		b[1] = (byte) (l >> 48);
		b[2] = (byte) (l >> 40);
		b[3] = (byte) (l >> 32);
		b[4] = (byte) (l >> 24);
		b[5] = (byte) (l >> 16);
		b[6] = (byte) (l >> 8);
		b[7] = (byte) l;
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
	
	public static final int intlongToBytes(int value, long lval, byte[] data) {
		int result = 1;
		
		data[0] = (byte)(value >>> 24);
		result = 31 * result + data[0];
		data[1] = (byte)(value >>> 16);
		result = 31 * result + data[1];
		data[2] = (byte)(value >>> 8);
		result = 31 * result + data[2];
		data[3] = (byte)value;
		result = 31 * result + data[3];
		data[4] = (byte)(lval >>> 56);
		result = 31 * result + data[4];
		data[5] = (byte)(lval >>> 48);
		result = 31 * result + data[5];
		data[6] = (byte)(lval >>> 40);
		result = 31 * result + data[6];
		data[7] = (byte)(lval >>> 32);
		result = 31 * result + data[7];
		data[8] = (byte)(lval >>> 24);
		result = 31 * result + data[8];
		data[9] = (byte)(lval >>> 16);
		result = 31 * result + data[9];
		data[10] = (byte)(lval >>> 8);
		result = 31 * result + data[10];
		data[11] = (byte)(lval);
		result = 31 * result + data[11];
		
		return result;
	}

	public static void writeNullable(long flag, Buffer buffer) {
		if (flag == 0l) {
			buffer.writeByte((byte) 0);
		} else {
			buffer.writeByte((byte) 1);
			buffer.writeLong(flag);
		}
	}

	public static long readNullable(Buffer buffer) {
		byte sign = buffer.readByte();
		if (sign == 1) {
			return buffer.readLong();
		}
		return 0l;
	}

	public static byte[] sha1(String value) {
		try {
			return SHA1.digest(value.getBytes("UTF-8"));
		} catch (Exception e) {
			throw Exceptions.runtime(e);
		}
	}

	public static long crc32(String value) {
		CRC32 crc = new CRC32();
		try {
			crc.update(value.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw Exceptions.runtime(e);
		}
		return crc.getValue();
	}
	
}
