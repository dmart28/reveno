package org.reveno.atp.clustering.core.messages;

import org.reveno.atp.clustering.api.message.Message;

public class NodeState extends Message {

    public static final int TYPE = 0xC5;

    @Override
    public int type() {
        return TYPE;
    }

    public long viewId;
    public long transactionId;
    public byte syncMode;
    public int syncPort;

    @Override
    public String toString() {
        return "NodeState{" +
                "viewId=" + viewId +
                ", transactionId=" + transactionId +
                ", syncMode=" + syncMode +
                ", syncPort=" + syncPort +
                '}';
    }

    public NodeState(long viewId, long transactionId, byte syncMode, int syncPort) {
        this.viewId = viewId;
        this.transactionId = transactionId;
        this.syncMode = syncMode;
        this.syncPort = syncPort;
    }

    public NodeState() {
    }
}
