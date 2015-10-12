package org.reveno.atp.clustering.core.jgroups;

import org.jgroups.Address;
import org.jgroups.Event;
import org.jgroups.JChannel;
import org.jgroups.PhysicalAddress;
import org.reveno.atp.clustering.api.InetAddress;
import org.reveno.atp.clustering.core.RevenoClusterConfiguration;
import org.reveno.atp.clustering.util.Tuple;

import java.net.UnknownHostException;
import java.util.List;
import java.util.stream.Collectors;

public abstract class JChannelHelper {

    public static InetAddress physicalAddress(JChannel channel, RevenoClusterConfiguration config,
                                          org.jgroups.Address address) {
        PhysicalAddress physicalAddress;
        while ((physicalAddress = (PhysicalAddress)
                channel.down(
                        new Event(Event.GET_PHYSICAL_ADDRESS, address)
                )) == null) {}

        String[] parts = physicalAddress.toString().split(":");
        java.net.InetAddress inet;
        try {
            inet = java.net.InetAddress.getByName(parts[0]);
        } catch (UnknownHostException e) {
            return null;
        }
        return config.clusterNodeAddresses().stream().map(a -> (InetAddress) a).filter(a -> a.getInetAddress().equals(inet)
                && a.getPort() == Integer.parseInt(parts[1])).findFirst().orElse(null);
    }
}
