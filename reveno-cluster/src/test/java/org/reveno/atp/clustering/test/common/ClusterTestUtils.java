package org.reveno.atp.clustering.test.common;

import com.google.common.io.Files;
import org.reveno.atp.clustering.api.Address;
import org.reveno.atp.clustering.api.IOMode;
import org.reveno.atp.clustering.api.InetAddress;
import org.reveno.atp.clustering.api.SyncMode;
import org.reveno.atp.clustering.core.buffer.ClusterProvider;
import org.reveno.atp.clustering.core.jgroups.JGroupsProvider;
import org.reveno.atp.clustering.util.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class ClusterTestUtils {

    public static List<ClusterEngineWrapper> createClusterEngines(int count, ClusterProvider provider) throws Exception {
        return createClusterEngines(count, Files.createTempDir(), e -> {}, provider);
    }

    public static List<ClusterEngineWrapper> createClusterEngines(int count, File dir, ClusterProvider provider) throws Exception {
        return createClusterEngines(count, dir, e -> {}, provider);
    }

    public static List<ClusterEngineWrapper> createClusterEngines(int count, Consumer<ClusterEngineWrapper> forEach,
                                                                  ClusterProvider provider) throws Exception {
        return createClusterEngines(count, Files.createTempDir(), forEach, provider);
    }

    public static List<ClusterEngineWrapper> createClusterEngines(int count, File dir,
                                                           Consumer<ClusterEngineWrapper> forEach, ClusterProvider provider) throws Exception {
        List<ClusterEngineWrapper> result = new ArrayList<>();
        int[] ports = Utils.getFreePorts(count * 2);
        for (int i = 0, p = 0; i < count; i++, p += 2) {
            ClusterEngineWrapper engine = new ClusterEngineWrapper(dir, ClusterTestUtils.class.getClassLoader(), provider);
            engine.setSequence(i);
            Address address = new InetAddress("127.0.0.1:" + ports[p], IOMode.SYNC);
            engine.clusterConfiguration().currentNodeAddress(address);
            engine.clusterConfiguration().priority(count - i);
            engine.clusterConfiguration().sync().port(ports[p + 1]);
            engine.clusterConfiguration().sync().mode(SyncMode.SNAPSHOT);

            forEach.accept(engine);

            result.add(engine);
        }

        for (ClusterEngineWrapper engine : result) {
            List<Address> nodes = result.stream()
                    .filter(subEngine -> engine != subEngine)
                    .map(ClusterEngineWrapper::getCurrentAddress)
                    .collect(Collectors.toList());
            engine.clusterConfiguration().clusterNodeAddresses(nodes);
        }
        return result;
    }

}
