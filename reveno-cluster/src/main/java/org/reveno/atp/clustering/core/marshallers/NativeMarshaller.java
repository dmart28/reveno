package org.reveno.atp.clustering.core.marshallers;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.reveno.atp.clustering.api.message.Marshaller;
import org.reveno.atp.clustering.api.message.Message;
import org.reveno.atp.clustering.core.messages.*;
import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.utils.Exceptions;

public class NativeMarshaller implements Marshaller {

    protected static final Int2ObjectMap<Class<? extends Message>> messages = new Int2ObjectOpenHashMap<>();

    static {
        messages.put(BarrierMessage.TYPE, BarrierMessage.class);
        messages.put(BarrierPassed.TYPE, BarrierPassed.class);
        messages.put(ForceElectionProcess.TYPE, ForceElectionProcess.class);
        messages.put(NodeState.TYPE, NodeState.class);
        messages.put(VoteAck.TYPE, VoteAck.class);
        messages.put(VoteMessage.TYPE, VoteMessage.class);
    }

    @Override
    public void marshall(Buffer buffer, Message message) {
        buffer.writeInt(message.type());
        message.write(buffer);
    }

    @Override
    public Message unmarshall(Buffer buffer) {
        Message message;
        try {
            message = messages.get(buffer.readInt()).newInstance();
            message.read(buffer);
        } catch (InstantiationException | IllegalAccessException e) {
            throw Exceptions.runtime(e);
        }
        return message;
    }
}
