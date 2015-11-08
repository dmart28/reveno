package org.reveno.atp.clustering.test.cluster_tests;

import org.junit.Test;
import org.reveno.atp.clustering.core.providers.UnicastAllProvider;
import org.reveno.atp.clustering.test.common.ClusterTestUtils;

public class OneNodeFailInMiddleTestJGroups extends ClusterBaseTest {

    @Test
    public void oneNodeFailInMiddleTestJGroups() throws Exception {
        setModelType();
        oneNodeFailInMiddleTest(() -> ClusterTestUtils.createClusterEngines(3, this::configure, UnicastAllProvider::new));
    }

}
