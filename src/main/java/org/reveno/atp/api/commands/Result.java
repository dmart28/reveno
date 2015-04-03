/** 
 *  Copyright (c) 2015 The original author or authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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
