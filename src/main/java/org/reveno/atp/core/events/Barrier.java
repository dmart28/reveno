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

public class Barrier implements Runnable {

	@Override
	public void run() {
		if (!isSuccessful) {
			eventPublisher.commitAsyncError(isReplay, transactionId);
		}
	}
	
	private boolean isOpen = false;
	public void open() {
		isOpen = true;
	}
	public boolean isOpen() {
		return isOpen;
	}
	
	private boolean isSuccessful = true;
	public void fail() {
		isSuccessful = false;
	}
	
	private final boolean isReplay;
	private final long transactionId;
	protected final EventPublisher eventPublisher;
	
	public Barrier(EventPublisher eventPublisher, long transactionId, boolean isReplay) {
		this.eventPublisher = eventPublisher;
		this.transactionId = transactionId;
		this.isReplay = isReplay;
	}
	
}
