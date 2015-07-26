package org.reveno.atp.core.api.channel;

import java.nio.ByteBuffer;


public interface Buffer {
	
	byte[] getBytes();

    int readerPosition();
    
    int writerPosition();

    int limit();

	long capacity();
	
	int length();
	
	int remaining();
	
	void clear();
	
	void release();
	
	boolean isAvailable();

    void setReaderPosition(int position);
    
    void setWriterPosition(int position);

    void setLimit(int limit);

    void writeByte(byte b);

	void writeBytes(byte[] bytes);

    void writeBytes(byte[] buffer, int offset, int count);
	
	void writeLong(long value);
	
	void writeInt(int value);

    void writeShort(short s);
	
	void writeFromBuffer(ByteBuffer buffer);

    ByteBuffer writeToBuffer();
	
	void writeFromBuffer(Buffer buffer);

    byte readByte();

	byte[] readBytes();
	
	byte[] readBytes(int length);
	
	void readBytes(byte[] data, int offset, int length);
	
	long readLong();
	
	int readInt();

    short readShort();

	
	void markReader();
	
	void markWriter();
	
	void resetReader();
	
	void resetWriter();
	
}
