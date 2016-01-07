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

import jdk.nashorn.internal.scripts.JO;
import org.reveno.atp.core.api.channel.Channel;

public interface JournalsStorage {

	Channel channel(String address);

	JournalStore[] getAllStores();

	JournalStore[] getStoresAfterVersion(long version);

	JournalStore[] getVolumes();

	void mergeStores(JournalStore[] stores, JournalStore to);

	void deleteStore(JournalStore store);

	JournalStore nextTempStore();

	JournalStore nextStore();

	JournalStore nextStore(long lastTxId);

	JournalStore nextVolume(long txSize, long eventsSize);

	JournalStore convertVolumeToStore(JournalStore volume);

	JournalStore convertVolumeToStore(JournalStore volume, long lastTxId);

	default JournalStore getLastStore() {
		JournalStore[] stores = getAllStores();
		return stores.length > 0 ? stores[stores.length - 1] : null;
	}

	default long getLastStoreVersion() {
		JournalStore store = getLastStore();
		return store == null ? 0 : store.getStoreVersion();
	}

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

		private final long storeVersion;
		public long getStoreVersion() {
			return storeVersion;
		}

		public JournalStore(String transactionCommitsAddress,
				String eventsCommitsAddress, long storeVersion, long lastTransactionId) {
			this.transactionCommitsAddress = transactionCommitsAddress;
			this.eventsCommitsAddress = eventsCommitsAddress;
			this.storeVersion = storeVersion;
			this.lastTransactionId = lastTransactionId;
		}

		@Override
		public int compareTo(JournalStore other) {
			return Long.compare(getStoreVersion(), other.getStoreVersion());
		}

		@Override
		public int hashCode() {
			return Long.hashCode(storeVersion);
		}
	}
}
