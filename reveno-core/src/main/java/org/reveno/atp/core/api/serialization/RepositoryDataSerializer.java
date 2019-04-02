package org.reveno.atp.core.api.serialization;

import org.reveno.atp.api.domain.RepositoryData;
import org.reveno.atp.core.api.channel.Buffer;

public interface RepositoryDataSerializer {

	int getSerializerType();

	void serialize(RepositoryData repository, Buffer buffer);
	
	RepositoryData deserialize(Buffer buffer);
	
}
