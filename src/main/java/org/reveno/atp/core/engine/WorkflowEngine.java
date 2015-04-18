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

package org.reveno.atp.core.engine;

import org.reveno.atp.core.engine.processor.PipeProcessor;

public class WorkflowEngine {
	
	public WorkflowEngine(PipeProcessor inputProcessor, PipeProcessor outputProcessor, 
			WorkflowContext context) {
		this.inputProcessor = inputProcessor;
		this.outputProcessor = outputProcessor;
		this.handlers = new InputHandlers(null); // TODO change
	}
	
	public void init() {
		inputProcessor.pipe(handlers::marshalling)
					.then(handlers::replication)
					.then(handlers::transactionExecution)
					.then(handlers::serialization)
					.then(handlers::journaling, handlers::viewsUpdate)
					.then(handlers::result);
	}
	
	protected void transaction() {
		
	}
	
	
	protected PipeProcessor inputProcessor;
	protected PipeProcessor outputProcessor;
	protected final InputHandlers handlers;
	
}
