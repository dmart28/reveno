package org.reveno.atp.api.transaction;

import org.reveno.atp.api.domain.WriteableRepository;

import java.util.Map;

public interface TransactionContext {
	
	EventBus eventBus();

	WriteableRepository repo();
	
	Map<Object, Object> data();
	
}
