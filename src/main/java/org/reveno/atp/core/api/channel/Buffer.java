package org.reveno.atp.core.api.channel;

import java.nio.ByteBuffer;


public interface Buffer {
	
	byte[] getBytes();
	
	int length();
	
	void clear();
	
	boolean isAvailable();
	
	void writeBytes(byte[] bytes);
	
	void writeLong(long value);
	
	void writeInt(int value);
	
	void writeFromBuffer(ByteBuffer buffer);
	
	byte[] readBytes();
	
	byte[] readBytes(int length);
	
	long readLong();
	
	int readInt();
	
	
	void mark();
	
	void reset();
	
}
