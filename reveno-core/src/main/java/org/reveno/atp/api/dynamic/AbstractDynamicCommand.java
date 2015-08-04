package org.reveno.atp.api.dynamic;

import java.util.Map;

public class AbstractDynamicCommand {

	protected Map<String, Object> args;
	public Map<String, Object> args() {
		return args;
	}
	public AbstractDynamicCommand args(Map<String, Object> args) {
		this.args = args;
		return this;
	}
	
	public AbstractDynamicCommand() {
	}
	
	public AbstractDynamicCommand(Map<String, Object> args) {
		this.args = args;
	}
	
}
