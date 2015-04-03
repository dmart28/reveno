package org.reveno.atp.core.api.serialization;

import org.reveno.atp.core.api.TransactionCommitInfo;
import org.reveno.atp.core.api.channel.Buffer;

public interface TransactionInfoSerializer {

	int getSerializerType();
	
	void registerTransactionType(Class<?> txDataType);
	
	void serialize(TransactionCommitInfo info, Buffer buffer);
	
	TransactionCommitInfo deserialize(TransactionCommitInfo.Builder builder, Buffer buffer);
	
}
