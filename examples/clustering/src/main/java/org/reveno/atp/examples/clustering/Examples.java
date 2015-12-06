package org.reveno.atp.examples.clustering;

import org.reveno.atp.clustering.api.ClusterConfiguration;
import org.reveno.atp.clustering.api.InetAddress;
import org.reveno.atp.clustering.core.ClusterEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

public class Examples {

    protected static final Logger LOG = LoggerFactory.getLogger(Examples.class);

    public static void main(String[] args) throws Exception {
        System.setProperty("java.net.preferIPv4Stack", "true");

        ClusterEngine[] clusterEngines = new ClusterEngine[2];
        InetAddress[] addresses = new InetAddress[]{new InetAddress("127.0.0.1:16343", "uid1"),
                new InetAddress("127.0.0.1:16344", "uid2")};

        clusterEngines[0] = new ClusterEngine(args[0] + "_1");
        clusterEngines[0].clusterConfiguration().currentNodeAddress(addresses[0]);
        clusterEngines[0].clusterConfiguration().nodesInetAddresses(Collections.singletonList(addresses[1]));
        // for real-time usage this port fetched automatically as defaultPort + 10
        // but since we have two engines in one VM, better to put it explicitly
        clusterEngines[0].clusterConfiguration().dataSync().port(16347);
        clusterEngines[1] = new ClusterEngine(args[0] + "_2");
        clusterEngines[1].clusterConfiguration().currentNodeAddress(addresses[1]);
        clusterEngines[1].clusterConfiguration().nodesInetAddresses(Collections.singletonList(addresses[0]));
        clusterEngines[1].clusterConfiguration().dataSync().port(16348);

        if (args[1].equals("multicast")) {
            multicast(clusterEngines);
        } else {
            unicast(clusterEngines);
        }

        clusterEngines[0].startup();
        clusterEngines[1].startup();

        // Engines could be used to issue commands, etc., as usual in all other examples,
        // just everything will be failovered from Master to Slave.
        // If you want to discover which node is master, then call engine.clusterStateInfo().isMaster()
        //
        // Please note that there will be leadership election process first, so engines will be
        // available for usage eventually.

        while (!(clusterEngines[0].clusterStateInfo().isMaster() ||
                clusterEngines[1].clusterStateInfo().isMaster())) {
            LOG.info("Still waiting leadership election to complete");
            Thread.sleep(1000);
        }
        LOG.info("Leadership election is completed!");
        if (clusterEngines[0].clusterStateInfo().isMaster()) {
            LOG.info("First node is master!");
        } else if (clusterEngines[1].clusterStateInfo().isMaster()) {
            LOG.info("Second node is master!");
        }
        if (clusterEngines[0].clusterStateInfo().isMaster() && clusterEngines[1].clusterStateInfo().isMaster()) {
            throw new IllegalStateException("Both masters - impossible situation.");
        }

        clusterEngines[0].shutdown();
        clusterEngines[1].shutdown();
    }

    public static void multicast(ClusterEngine[] clusterEngines) {
        for (ClusterEngine clusterEngine : clusterEngines) {
            clusterEngine.clusterConfiguration().commandsXmitTransport(ClusterConfiguration.CommandsXmitTransport.MULTICAST);
            clusterEngine.clusterConfiguration().multicast().host("229.10.31.87");
            clusterEngine.clusterConfiguration().multicast().port(13425);
            clusterEngine.clusterConfiguration().multicast().preferBatchingToLatency(false);
        }
    }

    public static void unicast(ClusterEngine[] clusterEngines) {
        for (ClusterEngine clusterEngine : clusterEngines) {
            // the one could use clusterEngine.clusterConfiguration().unicast() properties.
            // In this example here we just will leave all values as defaults, as for unicast
            // nothing special is required to start working with.
        }
    }

}
