package org.reveno.atp.clustering.core.messages;

import org.reveno.atp.clustering.api.message.Message;
import org.reveno.atp.core.api.channel.Buffer;

public class NodeState extends Message {

    public static final int TYPE = 0xC5;

    @Override
    public void write(Buffer buffer) {
        buffer.writeLong(viewId);
        buffer.writeLong(transactionId);
        buffer.writeByte(syncMode);
        buffer.writeInt(syncPort);
    }

    @Override
    public void read(Buffer buffer) {
        this.viewId = buffer.readLong();
        this.transactionId = buffer.readLong();
        this.syncMode = buffer.readByte();
        this.syncPort = buffer.readInt();
    }

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
