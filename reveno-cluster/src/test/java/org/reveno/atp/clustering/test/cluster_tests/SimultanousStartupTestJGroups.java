package org.reveno.atp.clustering.test.cluster_tests;

import org.junit.Test;
import org.reveno.atp.clustering.core.providers.UnicastAllProvider;
import org.reveno.atp.clustering.test.common.ClusterTestUtils;

public class SimultanousStartupTestJGroups extends ClusterBaseTest {

    @Test
    public void simultanousStartupTestJGroups() throws Exception {
        setModelType();
        simultanousStartupTest(() -> ClusterTestUtils.createClusterEngines(2, this::configure, UnicastAllProvider::new));
    }

}
