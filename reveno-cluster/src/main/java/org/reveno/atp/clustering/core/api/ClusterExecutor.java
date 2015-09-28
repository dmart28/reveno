package org.reveno.atp.clustering.core.api;

import org.reveno.atp.clustering.api.ClusterView;

public interface ClusterExecutor<T> {

    T execute(ClusterView currentView);

}
