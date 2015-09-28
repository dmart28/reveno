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

    @Override
    public String toString() {
        return "VoteMessage{" +
                "viewId=" + viewId +
                ", priority=" + priority +
                '}';
    }

    public VoteMessage(long viewId, int priority) {
        this.viewId = viewId;
        this.priority = priority;
    }

    public VoteMessage() {
    }

}
