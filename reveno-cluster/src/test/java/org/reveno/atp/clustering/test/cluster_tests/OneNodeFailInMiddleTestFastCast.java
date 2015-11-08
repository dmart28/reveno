package org.reveno.atp.clustering.test.cluster_tests;

import org.junit.Test;
import org.reveno.atp.clustering.core.providers.MulticastAllProvider;
import org.reveno.atp.clustering.test.common.ClusterEngineWrapper;
import org.reveno.atp.clustering.test.common.ClusterTestUtils;

import java.util.function.Consumer;

public class OneNodeFailInMiddleTestFastCast extends ClusterBaseTest {

    @Test
    public void oneNodeFailInMiddleTestFastCast() throws Exception {
        setModelType();
        Consumer<ClusterEngineWrapper> forEach = e -> {
            this.configure(e);
            e.clusterConfiguration().multicast().host("229.9.9.10");
            e.clusterConfiguration().multicast().port(47368);
            e.clusterConfiguration().multicast().sendRetries(Integer.MAX_VALUE);
        };
        oneNodeFailInMiddleTest(() -> ClusterTestUtils.createClusterEngines(3, forEach, MulticastAllProvider::new));
    }

}
