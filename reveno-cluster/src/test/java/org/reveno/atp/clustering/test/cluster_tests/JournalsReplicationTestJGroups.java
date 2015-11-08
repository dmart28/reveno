package org.reveno.atp.clustering.test.cluster_tests;

import org.junit.Test;
import org.reveno.atp.clustering.api.SyncMode;
import org.reveno.atp.clustering.core.providers.UnicastAllProvider;
import org.reveno.atp.clustering.test.common.ClusterEngineWrapper;
import org.reveno.atp.clustering.test.common.ClusterTestUtils;

import java.util.function.Consumer;

public class JournalsReplicationTestJGroups extends ClusterBaseTest {

    @Test
    public void journalsReplicationTestJGroups() throws Exception {
        setModelType();
        Consumer<ClusterEngineWrapper> forEach = e -> {
            this.configure(e);
            e.clusterConfiguration().dataSync().mode(SyncMode.JOURNALS);
        };
        basicTest(() -> ClusterTestUtils.createClusterEngines(2, forEach, UnicastAllProvider::new),
                forEach, UnicastAllProvider::new);
    }

}
