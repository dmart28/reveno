package org.reveno.atp.clustering.core.messages;

import org.reveno.atp.clustering.api.message.Message;
import org.reveno.atp.core.api.channel.Buffer;

import java.nio.charset.StandardCharsets;

public class BarrierMessage extends Message {

    public static final int TYPE = 0xA1;

    @Override
    public void write(Buffer buffer) {
        byte[] idBytes = id.getBytes(StandardCharsets.UTF_8);
        buffer.writeInt(idBytes.length);
        buffer.writeBytes(idBytes);
    }

    @Override
    public void read(Buffer buffer) {
        byte[] idBytes = new byte[buffer.readInt()];
        buffer.readBytes(idBytes, 0, idBytes.length);
        this.id = new String(idBytes);
    }

    public int type() {
        return TYPE;
    }

    public String id;

    public BarrierMessage(String id) {
        this.id = id;
    }

    public BarrierMessage() {
    }

}
