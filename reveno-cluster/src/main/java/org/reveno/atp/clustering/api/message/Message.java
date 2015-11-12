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

package org.reveno.atp.clustering.api.message;

import org.reveno.atp.clustering.api.Address;
import org.reveno.atp.core.api.channel.Buffer;

/**
 * General message instance which is used solely for
 * internal communications in Reveno, primarily in
 * leadership election process. Every {@link Message} insance should
 * contain unique {@link #type()}.
 */
public abstract class Message {

    /**
     * Provides an ability to serialize current message instance
     * data down to buffer.
     * @param buffer into which message will be serialized
     */
    public abstract void write(Buffer buffer);

    /**
     * Provides an ability to deserialize current message instance
     * data from the given buffer
     * @param buffer from which message will be read
     */
    public abstract void read(Buffer buffer);

    /**
     * Unique message type in the system.
     * @return
     */
    public abstract int type();

    private transient Address address;

    /**
     * Returns address from which this message was originally sent.
     * @return
     */
    public Address address() {
        return address;
    }
    public Message address(Address address) {
        this.address = address;
        return this;
    }

}