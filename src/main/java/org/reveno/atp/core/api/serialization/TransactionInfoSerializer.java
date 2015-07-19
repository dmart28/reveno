package org.reveno.atp.core.api.serialization;

import org.reveno.atp.core.api.TransactionCommitInfo;
import org.reveno.atp.core.api.channel.Buffer;

import java.util.List;

public interface TransactionInfoSerializer {

	int getSerializerType();
	
	void registerTransactionType(Class<?> txDataType);
	
	void serialize(TransactionCommitInfo info, Buffer buffer);
	
	TransactionCommitInfo deserialize(TransactionCommitInfo.Builder builder, Buffer buffer);
	
	void serializeCommands(List<Object> commands, Buffer buffer);
	
	List<Object> deserializeCommands(Buffer buffer);
	
}
