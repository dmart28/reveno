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
		private final boolean isRestore;
		private final long transactionTime;

		public EventMetadata(boolean isRestore, long transactionTime) {
			this.isRestore = isRestore;
			this.transactionTime = transactionTime;
		}

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

		/**
		 * Time at which transaction was executed and journaled in millis.
		 * @return time in milliseconds
		 */
		public long getTransactionTime() {
			return transactionTime;
		}

	}
	
}
