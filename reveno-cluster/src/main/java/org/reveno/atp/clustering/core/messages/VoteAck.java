package org.reveno.atp.clustering.core.messages;

import org.reveno.atp.clustering.api.message.Message;
import org.reveno.atp.core.api.channel.Buffer;

public class VoteAck extends Message {

    public static final int TYPE = 0xD3;

    @Override
    public void write(Buffer buffer) {
        buffer.writeLong(viewId);
    }

    @Override
    public void read(Buffer buffer) {
        this.viewId = buffer.readLong();
    }

    @Override
    public int type() {
        return TYPE;
    }

    public long viewId;

    @Override
    public String toString() {
        return "VoteAck{" +
                "viewId=" + viewId +
                '}';
    }

    public VoteAck(long viewId) {
        this.viewId = viewId;
    }

    public VoteAck() {
    }

}
