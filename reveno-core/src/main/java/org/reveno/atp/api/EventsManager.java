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

	/**
	 * Defines the size of internal thread pool used for async Event handlers.
	 * @param count
	 */
	void asyncEventExecutors(int count);

	/**
	 * Event handler that will be eventually executed by the internal thread pool on
	 * event fired from Transaction Actions.
	 *
	 * @param eventType
	 * @param consumer
	 * @param <E>
	 */
	<E> void asyncEventHandler(Class<E> eventType, BiConsumer<E, EventMetadata> consumer);

	/**
	 * Event handler that will be eventually executed by single thread with guaranteed ordering
	 * from Transaction Actions.
	 *
	 * @param eventType
	 * @param consumer
	 * @param <E>
	 */
	<E> void eventHandler(Class<E> eventType, BiConsumer<E, EventMetadata> consumer);
	
	<E> void removeEventHandler(Class<E> eventType, BiConsumer<E, EventMetadata> consumer);
	
	
	class EventMetadata {
		private boolean isRestore;

		/**
		 * Identifies whether it's the first time event is fired. Normally, events are not fired during
		 * replay if it previously successfully executed and was journaled. If one of this two points
		 * is not done, it will be fired again with flag set to true.
		 *
		 * @return {@code true} if event handler is fired during replay, otherwise {@code false}
		 */
		public boolean isRestore() {
			return isRestore;
		}
		
		private long transactionTime;

		/**
		 * Time at which transaction was executed and journaled in millis.
		 * @return time in milliseconds
		 */
		public long getTransactionTime() {
			return transactionTime;
		}
		
		public EventMetadata(boolean isRestore, long transactionTime) {
			this.isRestore = isRestore;
			this.transactionTime = transactionTime;
		}
	}
	
}
