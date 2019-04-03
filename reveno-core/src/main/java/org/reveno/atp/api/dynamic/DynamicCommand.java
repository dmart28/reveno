package org.reveno.atp.api.dynamic;

import java.util.Map;

public class DynamicCommand {
	protected final Class<? extends AbstractDynamicCommand> commandType;
	protected final Class<? extends AbstractDynamicTransaction> transactionType;

	public DynamicCommand(Class<? extends AbstractDynamicCommand> commandType, Class<? extends AbstractDynamicTransaction> transactionType) {
		this.commandType = commandType;
		this.transactionType = transactionType;
	}

	public Class<?> commandType() {
		return commandType;
	}

	public Class<?> transactionType() {
		return transactionType;
	}
	
	public AbstractDynamicCommand newCommand(Map<String, Object> args) throws InstantiationException, IllegalAccessException {
		return commandType.newInstance().args(args);
	}
	
}
