package org.reveno.atp.api.query;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;

public interface QueryManager {

	<V> V find(Class<V> viewType, long id);

	default <V> Optional<V> findO(Class<V> viewType, long id) {
		return Optional.ofNullable(find(viewType, id));
	}
	
	<V> Collection<V> select(Class<V> viewType);
	
	<V> Collection<V> select(Class<V> viewType, Predicate<V> filter);
	
	<V> Collection<V> parallelSelect(Class<V> viewType, Predicate<V> filter);
	
}
