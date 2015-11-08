package org.reveno.atp.clustering.core.messages;

import org.reveno.atp.clustering.api.message.Message;
import org.reveno.atp.core.api.channel.Buffer;

public class VoteMessage extends Message {

    public static final int TYPE = 0xE4;

    @Override
    public void write(Buffer buffer) {
        buffer.writeLong(viewId);
        buffer.writeInt(priority);
        buffer.writeLong(seed);
    }

    @Override
    public void read(Buffer buffer) {
        this.viewId = buffer.readLong();
        this.priority = buffer.readInt();
        this.seed = buffer.readLong();
    }

    @Override
    public int type() {
        return TYPE;
    }

    public long viewId;
    public int priority;
    public long seed;

    @Override
    public String toString() {
        return "VoteMessage{" +
                "viewId=" + viewId +
                ", priority=" + priority +
                ", seed=" + seed +
                '}';
    }

    public VoteMessage(long viewId, int priority, long seed) {
        this.viewId = viewId;
        this.priority = priority;
        this.seed = seed;
    }

    public VoteMessage() {
    }

}
