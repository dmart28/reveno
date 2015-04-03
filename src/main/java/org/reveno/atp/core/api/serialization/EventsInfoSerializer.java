package org.reveno.atp.core.api.serialization;

import org.reveno.atp.core.api.EventsCommitInfo;
import org.reveno.atp.core.api.channel.Buffer;

public interface EventsInfoSerializer {
	
	void serialize(EventsCommitInfo info, Buffer buffer);
	
	EventsCommitInfo deserialize(EventsCommitInfo.Builder builder, Buffer buffer);
	
}
