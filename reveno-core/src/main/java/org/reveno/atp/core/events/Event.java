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

package org.reveno.atp.core.events;

import org.reveno.atp.api.EventsManager.EventMetadata;
import org.reveno.atp.core.api.Destroyable;

import java.util.concurrent.CompletableFuture;

public class Event implements Destroyable {
	
	private boolean isAborted;
	public boolean isAborted() {
		return isAborted;
	}
	public void abort() {
		this.isAborted = true;
	}
	
	private boolean isReplay;
	public boolean isReplay() {
		return isReplay;
	}
	public Event replay(boolean isReplay) {
		this.isReplay = isReplay;
		return this;
	}
	
	private int flag;
	public int getFlag() {
		return flag;
	}
	public Event flag(int flag) {
		this.flag = flag;
		return this;
	}
	
	private CompletableFuture<?> syncFuture;
	public CompletableFuture<?> syncFuture() {
		return syncFuture;
	}
	public Event syncFuture(CompletableFuture<?> syncFuture) {
		this.syncFuture = syncFuture;
		return this;
	}
	
	private long transactionId;
	public long transactionId() {
		return transactionId;
	}
	public Event transactionId(long transactionId) {
		this.transactionId = transactionId;
		return this;
	}
	
	private Object[] events;
	public Object[] events() {
		return events;
	}
	public Event events(Object[] events) {
		this.events = events;
		return this;
	}
	
	private EventMetadata eventMetadata;
	public EventMetadata eventMetadata() {
		return eventMetadata;
	}
	public Event eventMetadata(EventMetadata eventMetadata) {
		this.eventMetadata = eventMetadata;
		return this;
	}
	
	public Event reset() {
		isAborted = false;
		isReplay = false;
		flag = 0;
		transactionId = 0L;
		syncFuture = null;
		events = null;
		eventMetadata = null;
		
		return this;
	}
	
	public void destroy() {
		reset();
	}
	
}
