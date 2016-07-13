package org.reveno.atp.clustering.test.cluster_tests;

import org.junit.Test;
import org.reveno.atp.clustering.core.providers.UnicastAllProvider;
import org.reveno.atp.clustering.test.common.ClusterEngineWrapper;
import org.reveno.atp.clustering.test.common.ClusterTestUtils;
import org.reveno.atp.utils.MeasureUtils;

import java.util.function.Consumer;

public class OneNodeFailInMiddleTestJGroups extends ClusterBaseTest {

    @Test
    public void oneNodeFailInMiddleTestJGroups() throws Exception {
        setModelType();
        Consumer<ClusterEngineWrapper> forEach = e -> {
            this.configure(e);
            e.clusterConfiguration().electionTimeouts().voteTimeoutNanos(MeasureUtils.sec(15));
        };
        oneNodeFailInMiddleTest(() -> ClusterTestUtils.createClusterEngines(3, forEach, UnicastAllProvider::new));
    }

}
