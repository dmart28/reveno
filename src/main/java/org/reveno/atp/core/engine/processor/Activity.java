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
import java.util.stream.IntStream;

public class Activity {

	private CompletableFuture<?> future;
	public CompletableFuture<?> getFuture() {
		return future;
	}
	public Activity future(CompletableFuture<?> future) {
		this.future = future;
		return this;
	}
	
	private boolean hasResult;
	public boolean hasResult() {
		return hasResult;
	}
	public Activity withResult() {
		this.hasResult = true;
		return this;
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
	public Activity replicated() {
		isReplicated = true;
		return this;
	}
	
	private List<Object> commands = new ArrayList<>();
	public List<Object> getCommands() {
		return commands;
	}
	public Activity addCommand(Object cmd) {
		commands.add(cmd);
		return this;
	}
	public Activity addCommands(Object[] cmds) {
		IntStream.range(0, cmds.length).forEach((i) -> commands.add(cmds[i]));
		return this;
	}
	
	
	public Activity reset() {
		commands.clear();
		hasResult = false;
		isAborted = false;
		future = null;
		
		return this;
	}
	
}
