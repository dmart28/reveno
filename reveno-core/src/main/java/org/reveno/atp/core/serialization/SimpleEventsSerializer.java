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
