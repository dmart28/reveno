package org.reveno.atp.clustering.core.components;

import org.reveno.atp.clustering.api.ClusterView;
import org.reveno.atp.clustering.api.message.Message;
import org.reveno.atp.clustering.core.api.ClusterExecutor;
import org.reveno.atp.clustering.core.api.MessagesReceiver;

import java.util.Set;

public class StorageTransferModelSync implements ClusterExecutor<Boolean>, MessagesReceiver {

    @Override
    public Boolean execute(ClusterView currentView) {
        return null;
    }

    @Override
    public <T extends Message> void onMessage(T message) {

    }

    @Override
    public Set<Integer> interestedTypes() {
        return null;
    }
}
