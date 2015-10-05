package org.reveno.atp.clustering.core.api;

import org.reveno.atp.clustering.api.message.Message;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public interface MessagesReceiver {

    void onMessage(Message message);

    Set<Integer> interestedTypes();

    default Optional<Predicate<Message>> filter() {
        return Optional.empty();
    }
}
