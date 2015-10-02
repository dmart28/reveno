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

package org.reveno.atp.core.api.storage;

import org.reveno.atp.core.api.channel.Channel;

public interface JournalsStorage {

	Channel channel(String address);

	JournalStore[] getAllStores();

	JournalStore[] getLastStores();

	JournalStore[] getVolumes();

	void mergeStores(JournalStore[] stores, JournalStore to);
	
	void deleteOldStores();

	JournalStore nextTempStore();

	JournalStore nextStore();

	JournalStore nextStore(long lastTxId);

	JournalStore nextVolume(long txSize, long eventsSize);

	JournalStore convertVolumeToStore(JournalStore volume);

	JournalStore convertVolumeToStore(JournalStore volume, long lastTxId);

	class JournalStore implements Comparable<JournalStore> {

		private final long lastTransactionId;
		public long getLastTransactionId() {
			return lastTransactionId;
		}

		private final String transactionCommitsAddress;
		public String getTransactionCommitsAddress() {
			return transactionCommitsAddress;
		}

		private final String eventsCommitsAddress;
		public String getEventsCommitsAddress() {
			return eventsCommitsAddress;
		}

		private final String storeVersion;
		public String getStoreVersion() {
			return storeVersion;
		}

		public JournalStore(String transactionCommitsAddress,
				String eventsCommitsAddress, String storeVersion, long lastTransactionId) {
			this.transactionCommitsAddress = transactionCommitsAddress;
			this.eventsCommitsAddress = eventsCommitsAddress;
			this.storeVersion = storeVersion;
			this.lastTransactionId = lastTransactionId;
		}

		@Override
		public int compareTo(JournalStore other) {
			return getStoreVersion().compareTo(other.getStoreVersion());
		}

		@Override
		public int hashCode() {
			return storeVersion.hashCode();
		}
	}
}
