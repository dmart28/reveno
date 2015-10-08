package org.reveno.atp.clustering.core.marshallers;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.reveno.atp.clustering.api.message.Marshaller;
import org.reveno.atp.clustering.api.message.Message;
import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.utils.Exceptions;

import java.io.IOException;

/**
 * Json Message marshaller. We can allow here to not bother yourself
 * with zero-copy approaches, since Marshaller is not critical part of engine,
 * and is used very rarely, and when it's a case, sub-microseconds precision is not
 * required.
 */
public class JsonMarshaller implements Marshaller {

    @Override
    public void marshall(Buffer buffer, Message message) {
        try {
            byte[] data = mapper.writeValueAsBytes(new MessageWrapper(message));
            buffer.writeInt(data.length);
            buffer.writeBytes(data);
        } catch (JsonProcessingException e) {
            throw Exceptions.runtime(e);
        }
    }

    @Override
    public Message unmarshall(Buffer buffer) {
        try {
            byte[] data = new byte[buffer.readInt()];
            buffer.readBytes(data, 0, data.length);
            return mapper.readValue(data, MessageWrapper.class).message;
        } catch (IOException e) {
            throw Exceptions.runtime(e);
        }
    }


    public JsonMarshaller() {
        this.mapper = new ObjectMapper();
        this.mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE);
        this.mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
    }

    protected ObjectMapper mapper;

    protected static class MessageWrapper {
        public Message message;

        public MessageWrapper(Message message) {
            this.message = message;
        }

        public MessageWrapper() {
        }
    }
}
