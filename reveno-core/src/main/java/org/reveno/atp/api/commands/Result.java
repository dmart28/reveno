package org.reveno.atp.api.commands;

public class Result<ResultType> {

	private final boolean success;
	public boolean isSuccess() {
		return success;
	}
	
	private final ResultType result;
	public ResultType getResult() {
		return result;
	}
	
	private final Throwable exception;
	public Throwable getException() {
		return exception;
	}
	
	
	public Result(ResultType result) {
		this(true, result, null);
	}
	
	public Result(Throwable exception) {
		this(false, null, exception);
	}
	
	public Result(boolean success, ResultType result, Throwable exception) {
		this.success = success;
		this.result = result;
		this.exception = exception;
	}
	
}
