package org.reveno.atp.clustering.core.api;

import org.reveno.atp.clustering.api.message.Message;

import java.util.Set;

public interface MessagesReceiver {

    <T extends Message> void onMessage(T message);

    Set<Integer> interestedTypes();

}
