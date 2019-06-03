package org.reveno.atp.core.api;

import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.api.channel.Channel;

import java.util.function.Consumer;

public interface Journaler {

    void startWriting(Channel ch);

    void stopWriting();

    Channel currentChannel();

    void writeData(Consumer<Buffer> journalerBuffer, boolean endOfBatch);

    void roll(Channel ch, Runnable rolled);

    void destroy();

}
