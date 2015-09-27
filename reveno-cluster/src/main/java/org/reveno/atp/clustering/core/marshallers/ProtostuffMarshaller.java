package org.reveno.atp.clustering.core.marshallers;

import org.reveno.atp.clustering.api.message.Marshaller;
import org.reveno.atp.clustering.api.message.Message;
import org.reveno.atp.core.api.channel.Buffer;

public class ProtostuffMarshaller implements Marshaller {

    @Override
    public void marshall(Buffer buffer, Message message) {

    }

    @Override
    public Message unmarshall(Buffer buffer) {
        return null;
    }

}
