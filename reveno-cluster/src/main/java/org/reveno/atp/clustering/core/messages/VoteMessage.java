package org.reveno.atp.clustering.core.messages;

import org.reveno.atp.clustering.api.message.Message;

public class VoteMessage extends Message {

    public static final int TYPE = 0xE4;

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
