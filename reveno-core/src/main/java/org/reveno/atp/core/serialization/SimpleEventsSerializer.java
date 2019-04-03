package org.reveno.atp.core.serialization;

import org.reveno.atp.api.exceptions.BufferOutOfBoundsException;
import org.reveno.atp.core.api.EventsCommitInfo;
import org.reveno.atp.core.api.EventsCommitInfo.Builder;
import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.api.serialization.EventsInfoSerializer;
import org.reveno.atp.utils.BinaryUtils;

public class SimpleEventsSerializer implements EventsInfoSerializer {

    @Override
    public void serialize(EventsCommitInfo info, Buffer buffer) {
        buffer.writeLong(info.transactionId());
        buffer.writeLong(info.time());
        BinaryUtils.writeNullable(info.flag(), buffer);
    }

    @Override
    public EventsCommitInfo deserialize(Builder builder, Buffer buffer) {
        EventsCommitInfo eci = builder.create(buffer.readLong(), buffer.readLong(), BinaryUtils.readNullable(buffer));
        if (eci.transactionId() == 0 && eci.time() == 0) {
            throw new BufferOutOfBoundsException();
        }
        return eci;
    }

}
