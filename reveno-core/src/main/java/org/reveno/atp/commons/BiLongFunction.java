package org.reveno.atp.commons;

@FunctionalInterface
public interface BiLongFunction<U, R> {

    R apply(long l, U u);

}
