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

package org.reveno.atp.core.disruptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.reveno.atp.api.EventsManager.EventMetadata;
import org.reveno.atp.api.transaction.EventBus;
import org.reveno.atp.core.api.Destroyable;
import org.reveno.atp.core.api.RestoreableEventBus;
import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.channel.NettyBasedBuffer;
import org.reveno.atp.utils.MapUtils;

import sun.misc.Contended;

@SuppressWarnings("rawtypes")
public class ProcessorContext implements Destroyable {
	
	private final RestoreableEventBus defaultEventBus = new ProcessContextEventBus();
	public EventBus defaultEventBus() {
		return defaultEventBus;
	}
	
	private long transactionId; 
	public long transactionId() {
		return transactionId;
	}
	public ProcessorContext transactionId(long transactionId) {
		this.transactionId = transactionId;
		return this;
	}

	private CompletableFuture future;
	public CompletableFuture future() {
		return future;
	}
	public ProcessorContext future(CompletableFuture future) {
		this.future = future;
		return this;
	}
	
	private Buffer marshallerBuffer = new NettyBasedBuffer(true);
	public Buffer marshallerBuffer() {
		return marshallerBuffer;
	}
	
	@Contended
	private Buffer transactionsBuffer = new NettyBasedBuffer(true);
	public Buffer transactionsBuffer() {
		return transactionsBuffer;
	}
	
	private boolean hasResult;
	public boolean hasResult() {
		return hasResult;
	}
	public ProcessorContext withResult() {
		this.hasResult = true;
		return this;
	}
	
	@Contended
	private Object commandResult;
	public Object commandResult() {
		return commandResult;
	}
	public void commandResult(Object commandResult) {
		this.commandResult = commandResult;
	}
	
	private boolean isAborted;
	private Throwable abortIssue;
	public boolean isAborted() {
		return isAborted;
	}
	public Throwable abortIssue() {
		return abortIssue;
	}
	public void abort(Throwable abortIssue) {
		this.isAborted = true;
		this.abortIssue = abortIssue;
	}
	
	private boolean isReplicated;
	public boolean isReplicated() {
		return isReplicated;
	}
	public ProcessorContext replicated() {
		isReplicated = true;
		return this;
	}
	
	private boolean isRestore;
	public boolean isRestore() {
		return isRestore;
	}
	public ProcessorContext restore() {
		isRestore = true;
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
	public ProcessorContext addTransactions(List<Object> transactions) {
		this.transactions.addAll(transactions);
		return this;
	}
	
	@Contended
	private List<Object> events = new ArrayList<>();
	public List<Object> getEvents() {
		return events;
	}
	
	private RestoreableEventBus eventBus = defaultEventBus;
	public RestoreableEventBus eventBus() {
		return eventBus;
	}
	public ProcessorContext eventBus(RestoreableEventBus eventBus) {
		this.eventBus = eventBus;
		return this;
	}
	
	private EventMetadata eventMetadata;
	public EventMetadata eventMetadata() {
		return eventMetadata;
	}
	public ProcessorContext eventMetadata(EventMetadata eventMetadata) {
		this.eventMetadata = eventMetadata;
		return this;
	}
	
	@Contended
	private Map<Class<?>, Set<Long>> markedRecords = MapUtils.repositoryLinkedSet();
	public Map<Class<?>, Set<Long>> getMarkedRecords() {
		return markedRecords;
	}
	
	
	public ProcessorContext reset() {
		transactionId = 0L;
		commands.clear();
		transactions.clear();
		events.clear();
		marshallerBuffer.clear();
		transactionsBuffer.clear();
		markedRecords.values().forEach(Set::clear);
		hasResult = false;
		isAborted = false;
		isRestore = false;
		isReplicated = false;
		abortIssue = null;
		future = null;
		commandResult = null;
		eventMetadata = null;
		eventBus = defaultEventBus;
		
		return this;
	}
	
	public void destroy() {
		reset();
		
		transactionsBuffer.release();
		marshallerBuffer.release();
	}
	
	protected class ProcessContextEventBus implements RestoreableEventBus {
		@Override
		public void publishEvent(Object event) {
			ProcessorContext.this.getEvents().add(event);
		}

		@Override
		public RestoreableEventBus currentTransactionId(long transactionId) {
			return this;
		}

		@Override
		public RestoreableEventBus underlyingEventBus(EventBus eventBus) {
			return this;
		}
	}
	
}
