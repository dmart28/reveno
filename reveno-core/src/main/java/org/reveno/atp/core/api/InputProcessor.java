package org.reveno.atp.core.api;

import org.reveno.atp.core.api.channel.Buffer;

import java.util.function.Consumer;

/**
 * Previosly we used to use Journaler for both reading and writing.
 * Such logic breaks the idea of Journaler. Its single responsibility is to give
 * comprehensive (or not) approach for writing.
 * <p>
 * We could simply put CQRS pattern here as well, segregating read and write parts.
 * InputProcessor will pay purpose of reading from stores.
 *
 * @author Artem Dmitriev <art.dm.ser@gmail.com>
 */
public interface InputProcessor extends AutoCloseable {

    void process(long fromVersion, Consumer<Buffer> consumer, JournalType type);

    enum JournalType {
        TRANSACTIONS, EVENTS
    }

}
