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

import java.util.function.BiConsumer;

import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.channel.NettyBasedBuffer;
import org.reveno.atp.core.engine.processor.ProcessorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InputHandlers {
	
	protected WorkflowContext services;

	public void ex(ProcessorContext c, boolean filter, boolean eob, BiConsumer<ProcessorContext, Boolean> body) {
		if (!c.isAborted() && filter) {
			try {
				body.accept(c, eob);
			} catch (Throwable t) {
				log.error("inputHandlers", t);
				c.abort();
			}
		} else {
			c.getFuture().complete(null);
		}
	}
	
	public void marshalling(ProcessorContext c, boolean endOfBatch) {
		ex(c, !c.isReplicated(), endOfBatch, marshaller);
	}
	protected final BiConsumer<ProcessorContext, Boolean> marshaller = (c, eob) -> {
		services.serializer().serializeCommands(c.getCommands(), c.marshallerBuffer());
	};
	
	public void replication(ProcessorContext c, boolean endOfBatch) {
		ex(c, c.marshallerBuffer().length() > 0, endOfBatch, replicator);
	}
	protected final BiConsumer<ProcessorContext, Boolean> replicator = (c, eob) -> {
		if (eob) {
			// TODO some replication here
		} else {
			marshalled.writeInt(c.marshallerBuffer().length());
			marshalled.writeFromBuffer(c.marshallerBuffer());
		}
	};
	
	public void transactionExecution(ProcessorContext c, boolean endOfBatch) {
		
	}
	
	public void serialization(ProcessorContext c, boolean endOfBatch) {
		
	}
	
	public void journaling(ProcessorContext c, boolean endOfBatch) {
		
	}
	
	public void viewsUpdate(ProcessorContext c, boolean endOfBatch) {
		
	}
	
	
	public InputHandlers(WorkflowContext context) {
		this.services = context;
	}
	
	
	protected static final Buffer marshalled = new NettyBasedBuffer(true);
	protected static final Logger log = LoggerFactory.getLogger(InputHandlers.class);
}
