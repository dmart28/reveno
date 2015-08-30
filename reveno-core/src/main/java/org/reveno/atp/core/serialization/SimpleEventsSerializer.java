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

package org.reveno.atp.core.serialization;

import org.reveno.atp.api.exceptions.BufferOutOfBoundsException;
import org.reveno.atp.core.api.EventsCommitInfo;
import org.reveno.atp.core.api.EventsCommitInfo.Builder;
import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.api.serialization.EventsInfoSerializer;

public class SimpleEventsSerializer implements EventsInfoSerializer {

	@Override
	public void serialize(EventsCommitInfo info, Buffer buffer) {
		buffer.writeLong(info.getTransactionId());
		buffer.writeLong(info.getTime());
		buffer.writeInt(info.getFlag());
	}

	@Override
	public EventsCommitInfo deserialize(Builder builder, Buffer buffer) {
		EventsCommitInfo eci = builder.create(buffer.readLong(), buffer.readLong(), buffer.readInt());
		if (eci.getTransactionId() == 0 && eci.getTime() == 0) {
			throw new BufferOutOfBoundsException();
		}
		return eci;
	}

}
