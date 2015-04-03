package org.reveno.atp.core.serialization;

import org.reveno.atp.core.api.EventsCommitInfo;
import org.reveno.atp.core.api.EventsCommitInfo.Builder;
import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.api.serialization.EventsInfoSerializer;

public class SimpleEventsSerializer implements EventsInfoSerializer {

	@Override
	public void serialize(EventsCommitInfo info, Buffer buffer) {
		buffer.writeLong(info.getTransactionId());
		buffer.writeLong(info.getTime());
	}

	@Override
	public EventsCommitInfo deserialize(Builder builder, Buffer buffer) {
		return builder.create(buffer.readLong(), buffer.readLong());
	}

}
