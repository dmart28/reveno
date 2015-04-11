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
import org.reveno.atp.core.engine.processor.ProcessorContext;

public class WorkflowEngine {
	
	public WorkflowEngine(PipeProcessor inputProcessor, PipeProcessor outputProcessor) {
		this.inputProcessor = inputProcessor;
		this.outputProcessor = outputProcessor;
	}
	
	public void init() {
		inputProcessor.pipe(inputHandlers::serialization)
					.then(inputHandlers::journaling, inputHandlers::replication)
					.then(inputHandlers::transactionExecution)
					.then(inputHandlers::viewsUpdate)
					.then(this::outputTransmitter);
		outputProcessor.pipe(outputHandlers::resultOutput, outputHandlers::publishEvents)
					.then(outputHandlers::serilalization)
					.then(outputHandlers::journaling);
	}
	
	protected void transaction() {
		
	}
	
	protected void outputTransmitter(ProcessorContext context, boolean endOfBatch) {
		
	}
	
	protected PipeProcessor inputProcessor;
	protected PipeProcessor outputProcessor;
	protected InputHandlers inputHandlers;
	protected OutputHandlers outputHandlers;
	
}
