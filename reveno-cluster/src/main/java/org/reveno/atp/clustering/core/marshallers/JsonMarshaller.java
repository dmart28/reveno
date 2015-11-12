/**
 *  Copyright (c) 2015 The original author or authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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
