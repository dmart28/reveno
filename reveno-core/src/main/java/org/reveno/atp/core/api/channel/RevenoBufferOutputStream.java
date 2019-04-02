package org.reveno.atp.core.api.channel;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Not used in core processing as such approach is very inefficient.
 * Currently, it's usage is limited only for zero-copy with ObjectOutputStream.
 */
public class RevenoBufferOutputStream extends OutputStream {
    protected final Buffer buffer ;

    public RevenoBufferOutputStream(Buffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public void write(int b) throws IOException {
        buffer.writeByte((byte) (b & 0xff));
    }
}
