package org.reveno.atp.clustering.core.messages;

import org.reveno.atp.clustering.api.message.Message;

public class BarrierMessage extends Message {

    public static final int TYPE = 0x1;

    public int type() {
        return TYPE;
    }

    public final String id;

    public BarrierMessage(String id) {
        this.id = id;
    }

}
