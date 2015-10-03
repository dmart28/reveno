package org.reveno.atp.clustering.core.api;

import org.reveno.atp.clustering.api.ClusterView;

public interface ClusterExecutor<R, ContextType> {

    R execute(ClusterView currentView, ContextType context);

    default R execute(ClusterView view) {
        return execute(view, null);
    }

}
