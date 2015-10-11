package org.reveno.atp.clustering.core.jgroups;

import org.jgroups.Address;
import org.jgroups.Event;
import org.jgroups.JChannel;
import org.jgroups.PhysicalAddress;
import org.reveno.atp.clustering.api.InetAddress;
import org.reveno.atp.clustering.core.RevenoClusterConfiguration;
import org.reveno.atp.clustering.util.Tuple;

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
        return config.clusterNodeAddresses().stream().map(a -> (InetAddress) a).filter(a -> a.getHost()
                .equals(parts[0]) && a.getPort() == Integer.parseInt(parts[1])).findFirst().orElse(null);
    }

    public static List<Address> physicalAddresses(JChannel channel, List<InetAddress> addresses) {
        return channel.getView().getMembers().stream().map(a -> new Tuple<>(a, (PhysicalAddress)
                channel.down(new Event(Event.GET_PHYSICAL_ADDRESS, a))))
                .filter(t -> addresses.stream().anyMatch(as -> as.toString().equals(t.getVal2().toString())))
                .map(Tuple::getVal1).collect(Collectors.toList());
    }

}
