package org.reveno.atp.api.commands.dynamic;

import java.util.Map;

public class DynamicCommand {

	protected Class<? extends AbstractCommand> commandType;
	public Class<?> commandType() {
		return commandType;
	}
	
	protected Class<? extends AbstractTransaction> transactionType;
	public Class<?> transactionType() {
		return transactionType;
	}
	
	public AbstractCommand newCommand(Map<String, Object> args) throws InstantiationException, IllegalAccessException {
		return commandType.newInstance().args(args);
	}
	
	public DynamicCommand(Class<? extends AbstractCommand> commandType, Class<? extends AbstractTransaction> transactionType) {
		this.commandType = commandType;
		this.transactionType = transactionType;
	}
	
}
