package org.reveno.atp.api.commands;

public class EmptyResult extends Result<Void> {
	
	public EmptyResult() {
		super(true, null, null);
	}
	
	public EmptyResult(Throwable exception) {
		super(false, null, exception);
	}

}
