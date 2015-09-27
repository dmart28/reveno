package org.reveno.atp.clustering.core.messages;

import org.reveno.atp.clustering.api.message.Message;

public class VoteMessage extends Message {

    public static final int TYPE = 0x4;

    @Override
    public int type() {
        return TYPE;
    }

    public final long viewId;
    public final int priority;

    public VoteMessage(long viewId, int priority) {
        this.viewId = viewId;
        this.priority = priority;
    }

}
