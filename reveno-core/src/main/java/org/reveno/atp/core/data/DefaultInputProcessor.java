package org.reveno.atp.core.data;

import org.reveno.atp.api.exceptions.BufferOutOfBoundsException;
import org.reveno.atp.core.api.InputProcessor;
import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.api.channel.Channel;
import org.reveno.atp.core.api.storage.JournalsStorage;
import org.reveno.atp.core.api.storage.JournalsStorage.JournalStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DefaultInputProcessor implements InputProcessor, Closeable {
    protected static final Logger log = LoggerFactory.getLogger(DefaultInputProcessor.class);

    protected JournalsStorage storage;

    public DefaultInputProcessor(JournalsStorage storage) {
        this.storage = storage;
    }

    @Override
    public void process(final long fromVersion, final Consumer<Buffer> consumer, JournalType type) {
        List<Channel> chs = Arrays.stream(stores(fromVersion)).map((js) -> type == JournalType.EVENTS ?
                js.getEventsCommitsAddress() : js.getTransactionCommitsAddress())
                .map(storage::channel).collect(Collectors.toList());
        ChannelReader bufferReader = new ChannelReader(chs);
        bufferReader.iterator().forEachRemaining(b -> {
            try {
                while (b.isAvailable()) {
                    consumer.accept(b);
                }
            } catch (BufferOutOfBoundsException ignored) {
                log.info("End of volume was reached ({})", b.readerPosition());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            } finally {
                if (b != null)
                    b.release();
            }
        });
    }

    @Override
    public void close() {
    }

    protected JournalStore[] stores(long fromVersion) {
        return storage.getStoresAfterVersion(fromVersion);
    }
}
