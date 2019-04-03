package org.reveno.atp.api.query;

import it.unimi.dsi.fastutil.longs.LongCollection;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

public interface MappingContext {

    <V> V get(Class<V> viewType, long id);

    default <V> Optional<V> getO(Class<V> viewType, long id) {
        return Optional.ofNullable(get(viewType, id));
    }

    <V> List<V> link(long[] ids, Class<V> viewType);

    <V> Set<V> linkSet(long[] ids, Class<V> viewType);

    <V> List<V> link(Stream<Long> ids, Class<V> viewType);

    <V> Set<V> linkSet(Stream<Long> ids, Class<V> viewType);

    <V> List<V> link(LongCollection ids, Class<V> viewType);

    <V> Set<V> linkSet(LongCollection ids, Class<V> viewType);

    <V> Supplier<V> link(Class<V> viewType, long id);

}
