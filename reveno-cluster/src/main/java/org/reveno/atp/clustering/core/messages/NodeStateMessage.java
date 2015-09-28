package org.reveno.atp.clustering.core.messages;

import org.reveno.atp.clustering.api.message.Message;

public class NodeStateMessage extends Message {

    public static final int TYPE = 0xC5;

    @Override
    public int type() {
        return TYPE;
    }

    public long viewId;
    public long transactionId;

    @Override
    public String toString() {
        return "NodeStateMessage{" +
                "viewId=" + viewId +
                ", transactionId=" + transactionId +
                '}';
    }

    public NodeStateMessage(long viewId, long transactionId) {
        this.viewId = viewId;
        this.transactionId = transactionId;
    }

    public NodeStateMessage() {
    }
}
