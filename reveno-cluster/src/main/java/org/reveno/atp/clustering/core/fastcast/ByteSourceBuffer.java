package org.reveno.atp.clustering.core.fastcast;

import org.nustaq.offheap.bytez.ByteSource;
import org.reveno.atp.core.api.channel.Buffer;

public class ByteSourceBuffer implements ByteSource {

    @Override
    public byte get(long index) {
        buffer.setReaderPosition((int)index);
        return buffer.readByte();
    }

    @Override
    public long length() {
        return buffer.limit();
    }

    public void setBuffer(Buffer buffer) {
        this.buffer = buffer;
    }

    protected Buffer buffer;
}
