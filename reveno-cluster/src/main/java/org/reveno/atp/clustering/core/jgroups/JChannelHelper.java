package org.reveno.atp.clustering.core.jgroups;

import org.jgroups.Address;
import org.jgroups.Event;
import org.jgroups.JChannel;
import org.jgroups.PhysicalAddress;
import org.reveno.atp.clustering.api.InetAddress;
import org.reveno.atp.clustering.core.RevenoClusterConfiguration;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class JChannelHelper {

    public static InetAddress physicalAddress(JChannel channel, RevenoClusterConfiguration config,
                                          org.jgroups.Address address) {
        PhysicalAddress physicalAddress = (PhysicalAddress)
                channel.down(
                        new Event(Event.GET_PHYSICAL_ADDRESS, address)
                );
        String[] parts = physicalAddress.toString().split(":");
        return config.clusterNodeAddresses().stream().map(a -> (InetAddress) a).filter(a -> a.getHost()
                .equals(parts[0]) && a.getPort() == Integer.parseInt(parts[1])).findFirst().orElse(null);
    }

    public static List<Address> physicalAddresses(JChannel channel, List<InetAddress> addresses) {
        return channel.getView().getMembers().stream().map(a -> (PhysicalAddress)
                channel.down(new Event(Event.GET_PHYSICAL_ADDRESS, a)))
                .filter(a -> addresses.stream().anyMatch(as -> as.toString().equals(a.toString())))
                .collect(Collectors.toList());
    }

}
