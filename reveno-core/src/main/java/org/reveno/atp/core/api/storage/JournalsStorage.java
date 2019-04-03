package org.reveno.atp.core.api.storage;

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
        private final String transactionCommitsAddress;
        private final String eventsCommitsAddress;
        private final long storeVersion;

        public JournalStore(String transactionCommitsAddress,
                            String eventsCommitsAddress, long storeVersion, long lastTransactionId) {
            this.transactionCommitsAddress = transactionCommitsAddress;
            this.eventsCommitsAddress = eventsCommitsAddress;
            this.storeVersion = storeVersion;
            this.lastTransactionId = lastTransactionId;
        }

        public long getLastTransactionId() {
            return lastTransactionId;
        }

        public String getTransactionCommitsAddress() {
            return transactionCommitsAddress;
        }

        public String getEventsCommitsAddress() {
            return eventsCommitsAddress;
        }

        public long getStoreVersion() {
            return storeVersion;
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
