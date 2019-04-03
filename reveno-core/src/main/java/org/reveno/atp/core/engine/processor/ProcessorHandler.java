package org.reveno.atp.core.engine.processor;

@FunctionalInterface
public interface ProcessorHandler<T> {

    void handle(T data, boolean endOfBatch);

}
