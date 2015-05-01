package org.reveno.atp.core.api.channel;

import java.nio.ByteBuffer;


public interface Buffer {
	
	byte[] getBytes();
	
	long capacity();
	
	int length();
	
	int remaining();
	
	void clear();
	
	void release();
	
	boolean isAvailable();
	
	void writeBytes(byte[] bytes);
	
	void writeLong(long value);
	
	void writeInt(int value);
	
	void writeFromBuffer(ByteBuffer buffer);
	
	void writeFromBuffer(Buffer buffer);
	
	byte[] readBytes();
	
	byte[] readBytes(int length);
	
	void readBytes(byte[] data, int offset, int length);
	
	long readLong();
	
	int readInt();
	
	
	void mark();
	
	void reset();
	
}
