/**
 *  Copyright (c) 2015 The original author or authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.reveno.atp.clustering.core.buffer;

import org.reveno.atp.clustering.api.Cluster;
import org.reveno.atp.clustering.api.ClusterBuffer;
import org.reveno.atp.clustering.core.RevenoClusterConfiguration;

/**
 * Factory for the two major (and possibly distinct in implementation)
 * things - {@link Cluster} and {@link ClusterBuffer}.
 */
public interface ClusterProvider {

    void initialize(RevenoClusterConfiguration config);

    Cluster retrieveCluster();

    ClusterBuffer retrieveBuffer();

}
