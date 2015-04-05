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

package org.reveno.atp.core.serialization.protostuff;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;

import com.dyuproject.protostuff.ByteString;
import com.dyuproject.protostuff.Input;
import com.dyuproject.protostuff.Output;
import com.dyuproject.protostuff.Schema;

public class InputOutputHolder implements Input, Output {
	
	public InputOutputHolder() {
		ints.add(new int[10]);
		longs.add(new long[10]);
		floats.add(new float[10]);
		doubles.add(new double[10]);
		bools.add(new boolean[10]);
	}

	@Override
	public void writeInt32(int fieldNumber, int value, boolean repeated)
			throws IOException {
		while (((double)fieldNumber / 10) >= ints.size()) 
			ints.add(new int[10]);
		ints.get(fieldNumber / 10)[fieldNumber % 10] = value;
		fields.add(fieldNumber);
	}

	@Override
	public void writeUInt32(int fieldNumber, int value, boolean repeated)
			throws IOException {
		writeInt32(fieldNumber, value, repeated);
	}

	@Override
	public void writeSInt32(int fieldNumber, int value, boolean repeated)
			throws IOException {
		writeInt32(fieldNumber, value, repeated);
	}

	@Override
	public void writeFixed32(int fieldNumber, int value, boolean repeated)
			throws IOException {
		writeInt32(fieldNumber, value, repeated);
	}

	@Override
	public void writeSFixed32(int fieldNumber, int value, boolean repeated)
			throws IOException {
		writeInt32(fieldNumber, value, repeated);
	}

	@Override
	public void writeInt64(int fieldNumber, long value, boolean repeated)
			throws IOException {
		while (((double)fieldNumber / 10) >= longs.size()) 
			longs.add(new long[10]);
		longs.get(fieldNumber / 10)[fieldNumber % 10] = value;
		fields.add(fieldNumber);
	}

	@Override
	public void writeUInt64(int fieldNumber, long value, boolean repeated)
			throws IOException {
		writeInt64(fieldNumber, value, repeated);
	}

	@Override
	public void writeSInt64(int fieldNumber, long value, boolean repeated)
			throws IOException {
		writeInt64(fieldNumber, value, repeated);
	}

	@Override
	public void writeFixed64(int fieldNumber, long value, boolean repeated)
			throws IOException {
		writeInt64(fieldNumber, value, repeated);
	}

	@Override
	public void writeSFixed64(int fieldNumber, long value, boolean repeated)
			throws IOException {
		writeInt64(fieldNumber, value, repeated);
	}

	@Override
	public void writeFloat(int fieldNumber, float value, boolean repeated)
			throws IOException {
		while (((double)fieldNumber / 10) >= floats.size()) 
			floats.add(new float[10]);
		floats.get(fieldNumber / 10)[fieldNumber % 10] = value;
		fields.add(fieldNumber);
	}

	@Override
	public void writeDouble(int fieldNumber, double value, boolean repeated)
			throws IOException {
		while (((double)fieldNumber / 10) >= doubles.size()) 
			doubles.add(new double[10]);
		doubles.get(fieldNumber / 10)[fieldNumber % 10] = value;
		fields.add(fieldNumber);
	}

	@Override
	public void writeBool(int fieldNumber, boolean value, boolean repeated)
			throws IOException {
		while (((double)fieldNumber / 10) >= bools.size()) 
			bools.add(new boolean[10]);
		bools.get(fieldNumber / 10)[fieldNumber % 10] = value;
		fields.add(fieldNumber);
	}

	@Override
	public void writeEnum(int fieldNumber, int value, boolean repeated)
			throws IOException {
		writeInt32(fieldNumber, value, repeated);
	}

	@Override
	public void writeString(int fieldNumber, String value, boolean repeated)
			throws IOException {
		insert(fieldNumber, value);
	}

	@Override
	public void writeBytes(int fieldNumber, ByteString value, boolean repeated)
			throws IOException {
		insert(fieldNumber, value);
	}

	@Override
	public void writeByteArray(int fieldNumber, byte[] value, boolean repeated)
			throws IOException {
		insert(fieldNumber, value);
	}

	@Override
	public void writeByteRange(boolean utf8String, int fieldNumber,
			byte[] value, int offset, int length, boolean repeated)
			throws IOException {
		System.err.println("!");
	}

	@Override
	public <T> void writeObject(int fieldNumber, T value, Schema<T> schema,
			boolean repeated) throws IOException {
		InputOutputHolder cb = new InputOutputHolder();
		try {
			schema.writeTo(cb, value);
		} catch (Throwable t) {
			t.printStackTrace();
		}
		insert(fieldNumber, cb);
	}
	
	private void insert(int fieldNumber, Object value) {
		fields.add(fieldNumber);
		data.add(value);
	}
	
	/// -----------------------------------------------------------------

	@Override
	public <T> void handleUnknownField(int fieldNumber, Schema<T> schema)
			throws IOException {
		System.err.println(fieldNumber + ": " + schema);
	}

	@Override
	public <T> int readFieldNumber(Schema<T> schema) throws IOException {
		int val = 0;
		try {
			val = fields.remove();
		} catch (NoSuchElementException e) {}
		this.currentField = val;
		return val;
	}

	@Override
	public int readInt32() throws IOException {
		return ints.get(currentField / 10)[currentField % 10];
	}

	@Override
	public int readUInt32() throws IOException {
		return readInt32();
	}

	@Override
	public int readSInt32() throws IOException {
		return readInt32();
	}

	@Override
	public int readFixed32() throws IOException {
		return readInt32();
	}

	@Override
	public int readSFixed32() throws IOException {
		return readInt32();
	}

	@Override
	public long readInt64() throws IOException {
		return longs.get(currentField / 10)[currentField % 10];
	}

	@Override
	public long readUInt64() throws IOException {
		return readInt64();
	}

	@Override
	public long readSInt64() throws IOException {
		return readInt64();
	}

	@Override
	public long readFixed64() throws IOException {
		return readInt64();
	}

	@Override
	public long readSFixed64() throws IOException {
		return readInt64();
	}

	@Override
	public float readFloat() throws IOException {
		return floats.get(currentField / 10)[currentField % 10]; 
	}

	@Override
	public double readDouble() throws IOException {
		return doubles.get(currentField / 10)[currentField % 10]; 
	}

	@Override
	public boolean readBool() throws IOException {
		return bools.get(currentField / 10)[currentField % 10]; 
	}

	@Override
	public int readEnum() throws IOException {
		return readInt32();
	}

	@Override
	public String readString() throws IOException {
		return (String) data.remove();
	}

	@Override
	public ByteString readBytes() throws IOException {
		return (ByteString) data.remove();
	}

	@Override
	public byte[] readByteArray() throws IOException {
		return (byte[]) data.remove();
	}

	@Override
	public <T> T mergeObject(T value, Schema<T> schema) throws IOException {
		InputOutputHolder cb = (InputOutputHolder) data.remove();
		try {
			schema.mergeFrom(cb, value);
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return value;
	}

	@Override
	public void transferByteRangeTo(Output output, boolean utf8String,
			int fieldNumber, boolean repeated) throws IOException {
		System.err.println("rd !");
	}

	public void clear() {
		data.clear();
		ints.clear();
		longs.clear();
		floats.clear();
		doubles.clear();
		bools.clear();
		fields.clear();
	}
	
	protected int currentField = 0;
	protected List<int[]> ints = new ArrayList<int[]>();
	protected List<long[]> longs = new ArrayList<long[]>();
	protected List<float[]> floats = new ArrayList<float[]>();
	protected List<double[]> doubles = new ArrayList<double[]>();
	protected List<boolean[]> bools = new ArrayList<boolean[]>();
	protected Queue<Object> data = new LinkedList<Object>();
	protected Queue<Integer> fields = new LinkedList<Integer>();
}

