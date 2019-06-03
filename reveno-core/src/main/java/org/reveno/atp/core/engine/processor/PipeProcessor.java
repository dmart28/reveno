package org.reveno.atp.core.engine.processor;

import org.reveno.atp.core.api.Destroyable;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

@SuppressWarnings("unchecked")
public interface PipeProcessor<T extends Destroyable> {

    long SYNC_FLAG = 0x111;

    void start();

    void stop();

    void sync();

    void shutdown();

    boolean isStarted();

    PipeProcessor<T> pipe(ProcessorHandler<T>... handler);

    <R> CompletableFuture<R> process(BiConsumer<T, CompletableFuture<R>> consumer);

    default PipeProcessor<T> then(ProcessorHandler<T>... handler) {
        return pipe(handler);
    }

}
