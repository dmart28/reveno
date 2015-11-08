package org.reveno.atp.clustering.core.jgroups;

import org.jgroups.Event;
import org.jgroups.JChannel;
import org.jgroups.PhysicalAddress;
import org.reveno.atp.clustering.api.InetAddress;
import org.reveno.atp.clustering.core.RevenoClusterConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;

public abstract class JChannelHelper {

    protected static final Logger LOG = LoggerFactory.getLogger(JChannelHelper.class);

    public static InetAddress physicalAddress(JChannel channel, RevenoClusterConfiguration config,
                                          org.jgroups.Address address) {
        PhysicalAddress physicalAddress;
        long count = 0;
        while ((physicalAddress = (PhysicalAddress)
                channel.down(
                        new Event(Event.GET_PHYSICAL_ADDRESS, address)
                )) == null) {
            if (count++ % 1000 == 0) {
                LOG.error("Still can't retrieve physical address of node");
            }
        }

        String[] parts = physicalAddress.toString().split(":");
        java.net.InetAddress inet;
        try {
            inet = java.net.InetAddress.getByName(parts[0]);
        } catch (UnknownHostException e) {
            return null;
        }
        return config.clusterNodeAddresses().stream().map(a -> (InetAddress) a).filter(a -> a.getHost().equals(inet.getHostAddress())
                && a.getPort() == Integer.parseInt(parts[1])).findFirst().orElse(null);
    }
}
