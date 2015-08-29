package org.reveno.atp.core.api.channel;

import java.io.IOException;
import java.io.OutputStream;

public class RevenoBufferOutputStream extends OutputStream {

    protected final Buffer buffer ;

    @Override
    public void write(int b) throws IOException {
        buffer.writeByte((byte) (b & 0xff));
    }

    public RevenoBufferOutputStream(Buffer buffer) {
        this.buffer = buffer;
    }

}
