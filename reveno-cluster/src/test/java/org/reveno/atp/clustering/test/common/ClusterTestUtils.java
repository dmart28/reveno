package org.reveno.atp.clustering.test.common;

import com.google.common.io.Files;
import org.reveno.atp.clustering.api.Address;
import org.reveno.atp.clustering.api.IOMode;
import org.reveno.atp.clustering.api.InetAddress;
import org.reveno.atp.clustering.api.SyncMode;
import org.reveno.atp.clustering.core.buffer.ClusterProvider;
import org.reveno.atp.clustering.util.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class ClusterTestUtils {

    public static List<ClusterEngineWrapper> createClusterEngines(int count, Supplier<ClusterProvider> provider) {
        return createClusterEngines(count, randomFiles(count), e -> {}, provider);
    }

    public static List<ClusterEngineWrapper> createClusterEngines(int count, List<File> dir, Supplier<ClusterProvider> provider) {
        return createClusterEngines(count, dir, e -> {}, provider);
    }

    public static List<ClusterEngineWrapper> createClusterEngines(int count, Consumer<ClusterEngineWrapper> forEach,
                                                                  Supplier<ClusterProvider> provider) {
        return createClusterEngines(count, randomFiles(count), forEach, provider);
    }

    public static List<ClusterEngineWrapper> createClusterEngines(int count, List<File> dir,
                                                           Consumer<ClusterEngineWrapper> forEach, Supplier<ClusterProvider> provider) {
        List<ClusterEngineWrapper> result = new ArrayList<>();
        int[] ports = Utils.getFreePorts(count * 2);
        for (int i = 0, p = 0; i < count; i++, p += 2) {
            ClusterEngineWrapper engine = new ClusterEngineWrapper(dir.get(i), ClusterTestUtils.class.getClassLoader(), provider.get());
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

    public static List<File> randomFiles(int count) {
        return Stream.generate(Files::createTempDir).limit(count).collect(Collectors.toList());
    }

}
