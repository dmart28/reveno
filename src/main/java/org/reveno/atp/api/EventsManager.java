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

package org.reveno.atp.api;

import java.util.function.BiConsumer;

public interface EventsManager {
	
	void asyncEventExecutors(int count);
	
	<E> void asyncEventHandler(Class<E> eventType, BiConsumer<E, EventMetadata> consumer);
	
	<E> void eventHandler(Class<E> eventType, BiConsumer<E, EventMetadata> consumer);
	
	<E> void removeEventHandler(Class<E> eventType, BiConsumer<E, EventMetadata> consumer);
	
	
	public static class EventMetadata {
		private boolean isRestore;
		public boolean isRestore() {
			return isRestore;
		}
		
		private long transactionTime;
		public long getTransactionTime() {
			return transactionTime;
		}
		
		public EventMetadata(boolean isRestore, long transactionTime) {
			this.isRestore = isRestore;
			this.transactionTime = transactionTime;
		}
	}
	
}
