package org.reveno.atp.api.query;

import java.util.Optional;

public interface ViewsRepository {

	<V> Optional<V> get(Class<V> viewType, long id);
	
}
