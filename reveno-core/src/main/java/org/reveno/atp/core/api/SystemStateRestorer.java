package org.reveno.atp.core.api;

public interface SystemStateRestorer {

    SystemState restore(long fromVersion, TxRepository repository);


    class SystemState {
        private long lastTransactionId;

        public SystemState(long lastTransactionId) {
            this.lastTransactionId = lastTransactionId;
        }

        public long getLastTransactionId() {
            return lastTransactionId;
        }
    }

}
