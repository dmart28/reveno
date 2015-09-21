package org.reveno.atp.clustering.core.messages;

import org.reveno.atp.clustering.api.message.Message;

public class BarrierPassed extends Message {

    public static final int TYPE = 0x2;

    public int type() {
        return TYPE;
    }

    public String id;

    public BarrierPassed(String id) {
        this.id = id;
    }

}
