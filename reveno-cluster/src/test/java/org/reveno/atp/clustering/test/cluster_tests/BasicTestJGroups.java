package org.reveno.atp.clustering.test.cluster_tests;

import org.junit.Test;
import org.reveno.atp.clustering.core.providers.UnicastAllProvider;
import org.reveno.atp.clustering.test.common.ClusterTestUtils;

public class BasicTestJGroups extends ClusterBaseTest {

    @Test
    public void basicTestJGroups() throws Exception {
        setModelType();
        basicTest(() -> ClusterTestUtils.createClusterEngines(2, this::configure, UnicastAllProvider::new),
                this::configure, UnicastAllProvider::new);
    }

}
