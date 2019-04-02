package org.reveno.atp.core.api;

import org.reveno.atp.api.domain.RepositoryData;

@FunctionalInterface
public interface TxRepositoryFactory {
	
	TxRepository create(RepositoryData data);
	
}
