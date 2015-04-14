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

package org.reveno.atp.core.engine.processor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.reveno.atp.core.api.channel.Buffer;

public class ProcessorContext {

	private CompletableFuture<?> future;
	public CompletableFuture<?> getFuture() {
		return future;
	}
	public ProcessorContext future(CompletableFuture<?> future) {
		this.future = future;
		return this;
	}
	
	private Buffer marshallerBuffer;
	public Buffer marshallerBuffer() {
		return marshallerBuffer;
	}
	
	private boolean journalingSuccess;
	public boolean isJournalingSuccess() {
		return journalingSuccess;
	}
	public ProcessorContext journalingSuccessful() {
		this.journalingSuccess = true;
		return this;
	}
	
	private boolean replicationSuccess;
	public boolean isReplicationSuccess() {
		return replicationSuccess;
	}
	public ProcessorContext replicationSuccessful() {
		this.replicationSuccess = true;
		return this;
	}
	
	private boolean hasResult;
	public boolean hasResult() {
		return hasResult;
	}
	public ProcessorContext withResult() {
		this.hasResult = true;
		return this;
	}
	private Object commandResult;
	public Object commandResult() {
		return commandResult;
	}
	public void commandResult(Object commandResult) {
		this.commandResult = commandResult;
	}
	
	private boolean isAborted;
	public boolean isAborted() {
		return isAborted;
	}
	public void abort() {
		this.isAborted = true;
	}
	
	private boolean isReplicated;
	public boolean isReplicated() {
		return isReplicated;
	}
	public ProcessorContext replicated() {
		isReplicated = true;
		return this;
	}
	
	private List<Object> commands = new ArrayList<>();
	public List<Object> getCommands() {
		return commands;
	}
	public ProcessorContext addCommand(Object cmd) {
		commands.add(cmd);
		return this;
	}
	public ProcessorContext addCommands(List<Object> cmds) {
		commands.addAll(cmds);
		return this;
	}
	
	private List<Object> transactions = new ArrayList<>();
	public List<Object> getTransactions() {
		return transactions;
	}
	
	
	public ProcessorContext reset() {
		commands.clear();
		marshallerBuffer.clear();
		hasResult = false;
		isAborted = false;
		isReplicated = false;
		journalingSuccess = false;
		replicationSuccess = false;
		future = null;
		commandResult = null;
		
		return this;
	}
	
}
