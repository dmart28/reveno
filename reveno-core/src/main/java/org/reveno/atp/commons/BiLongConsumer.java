package org.reveno.atp.commons;

@FunctionalInterface
public interface BiLongConsumer<T> {

    void accept(long l, T t);

}
