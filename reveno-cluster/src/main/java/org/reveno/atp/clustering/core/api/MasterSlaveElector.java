package org.reveno.atp.clustering.core.api;

import org.reveno.atp.clustering.api.ClusterView;

public interface MasterSlaveElector extends MessagesReceiver {

    ElectionResult vote(ClusterView currentView);


    class ElectionResult {
        public final boolean isMaster;
        public final boolean failed;

        public ElectionResult(boolean master, boolean failed) {
            this.isMaster = master;
            this.failed = failed;
        }
    }
}
