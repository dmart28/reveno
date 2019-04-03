package org.reveno.atp.core.api.channel;

import java.util.function.Consumer;

public interface Channel extends AutoCloseable {

    long size();

    long position();

    boolean isReadAvailable();

    Buffer read();

    void write(Consumer<Buffer> channelBuffer, boolean flush);

    boolean isOpen();

    void close();

}
