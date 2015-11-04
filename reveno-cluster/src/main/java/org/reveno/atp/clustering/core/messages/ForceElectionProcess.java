package org.reveno.atp.clustering.core.messages;

import org.reveno.atp.clustering.api.message.Message;
import org.reveno.atp.core.api.channel.Buffer;

public class ForceElectionProcess extends Message {

    public static final int TYPE = 0x3FAB;

    @Override
    public void write(Buffer buffer) {
    }

    @Override
    public void read(Buffer buffer) {
    }

    @Override
    public int type() {
        return TYPE;
    }
}
