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
