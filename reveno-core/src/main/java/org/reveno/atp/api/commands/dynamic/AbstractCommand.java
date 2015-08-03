package org.reveno.atp.api.commands.dynamic;

import java.util.Map;

public class AbstractCommand {

	protected Map<String, Object> args;
	public Map<String, Object> args() {
		return args;
	}
	public AbstractCommand args(Map<String, Object> args) {
		this.args = args;
		return this;
	}
	
	public AbstractCommand() {
	}
	
	public AbstractCommand(Map<String, Object> args) {
		this.args = args;
	}
	
}
