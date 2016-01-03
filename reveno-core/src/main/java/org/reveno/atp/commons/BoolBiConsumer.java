package org.reveno.atp.commons;

import java.util.Objects;

@FunctionalInterface
public interface BoolBiConsumer<T> {

    void accept(T t, boolean b);

    default BoolBiConsumer<T> andThen(BoolBiConsumer<? super T> after) {
        Objects.requireNonNull(after);

        return (l, r) -> {
            accept(l, r);
            after.accept(l, r);
        };
    }
}