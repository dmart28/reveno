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


package org.reveno.atp.clustering.core.messages;

import org.reveno.atp.clustering.api.message.Message;
import org.reveno.atp.core.api.channel.Buffer;

public class VoteMessage extends Message {

    public static final int TYPE = 0xE4;

    @Override
    public void write(Buffer buffer) {
        buffer.writeLong(viewId);
        buffer.writeInt(priority);
        buffer.writeLong(seed);
    }

    @Override
    public void read(Buffer buffer) {
        this.viewId = buffer.readLong();
        this.priority = buffer.readInt();
        this.seed = buffer.readLong();
    }

    @Override
    public int type() {
        return TYPE;
    }

    public long viewId;
    public int priority;
    public long seed;

    @Override
    public String toString() {
        return "VoteMessage{" +
                "viewId=" + viewId +
                ", priority=" + priority +
                ", seed=" + seed +
                '}';
    }

    public VoteMessage(long viewId, int priority, long seed) {
        this.viewId = viewId;
        this.priority = priority;
        this.seed = seed;
    }

    public VoteMessage() {
    }

}
