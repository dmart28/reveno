package org.reveno.atp.api.exceptions;

public class IllegalFileName extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public IllegalFileName(String fileName, Throwable t) {
		super(String.format("Versioned file name can't be parsed [%s]", fileName), t);
	}

}
