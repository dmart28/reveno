package org.reveno.atp.api.dynamic;

import java.util.Map;

public class DynamicCommand {

	protected Class<? extends AbstractDynamicCommand> commandType;
	public Class<?> commandType() {
		return commandType;
	}
	
	protected Class<? extends AbstractDynamicTransaction> transactionType;
	public Class<?> transactionType() {
		return transactionType;
	}
	
	public AbstractDynamicCommand newCommand(Map<String, Object> args) throws InstantiationException, IllegalAccessException {
		return commandType.newInstance().args(args);
	}
	
	public DynamicCommand(Class<? extends AbstractDynamicCommand> commandType, Class<? extends AbstractDynamicTransaction> transactionType) {
		this.commandType = commandType;
		this.transactionType = transactionType;
	}
	
}
