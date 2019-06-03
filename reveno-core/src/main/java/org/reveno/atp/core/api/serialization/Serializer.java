package org.reveno.atp.core.api.serialization;

import org.reveno.atp.core.api.channel.Buffer;

public interface Serializer {

    int getSerializerType();

    boolean isRegistered(Class<?> type);

    void registerTransactionType(Class<?> txDataType);

    void serializeObject(Buffer buffer, Object tc);

    Object deserializeObject(Buffer buffer);

}
