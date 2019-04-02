package org.reveno.atp.api.commands;

import org.reveno.atp.api.domain.Repository;

public interface CommandContext {

	Repository repo();
	
	long id(Class<?> entityType);

	CommandContext executeTxAction(Object transactionAction);

	@Deprecated
	/*
	  Use {@link #executeTxAction(Object)} instead.
	 */
	CommandContext executeTransaction(Object transactionAction);
	
}
