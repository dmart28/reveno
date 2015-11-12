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

package org.reveno.atp.clustering.api;

import org.reveno.atp.clustering.api.message.Marshaller;
import org.reveno.atp.clustering.api.message.Message;

import java.util.function.Consumer;

/**
 * In Reveno, Cluster is an interface which denotes the notion of
 * group of Reveno hosts, which works according to Master-Slave principle.
 * According to it, there is single Master host, which receives incoming Transaction Commands and
 * some number of Slave ones, which receives Commands in failover mode from Master, means they
 * produce no events and just keeps the same state as Master, so, in case of any Master
 * failure, they can re-elect new leader, which will start from the same point the previous
 * Master node had stopped.
 *
 * In Reveno clustering ecosystem, the whole failover engine is separeted into two big parts:
 *  - Coordination part
 *  - Failover part
 *
 * {@link Cluster} interface is the part of "Coordination part", which plays next roles:
 *  - Connect/Disconnect to other nodes
 *  - Keep track of current cluster members via {@link ClusterView}
 *  - Perform leadership election in case of any cluster membership changes
 *  - Prevent cluster from "split-brain" phenomen.
 *  - Provide internal system messaging between nodes.
 */
public interface Cluster {

	/**
	 * Connects to the current Reveno group of nodes. Depending on the
	 * concrete implementation, either physical connection is opened (TCP protocol for example),
	 * or not (UPD protocol for example). In any case, it allows to performs much of
	 * initialization things.
	 */
	void connect();

	/**
	 * Marks itself as detached from the cluster, and frees all allocated resources and connections
	 * for it.
	 */
	void disconnect();

	/**
	 * Checks whether current node is included in current group of hosts.
	 * @return
	 */
	boolean isConnected();

	/**
	 * Provides gateway instance which provides with communication ability between nodes via
	 * sending {@link Message}s to each other.
	 * @return
	 */
	ClusterConnector gateway();

	/**
	 * Sets the marshaller instance, which will be used to marshall/unmarshall all incoming/outgoing
	 * messages. Surely, all nodes in cluster should use the same one.
	 * @param marshaller
	 */
	void marshallWith(Marshaller marshaller);

	/**
	 * Subscribes to the set of events, which can eventually be issued, reflecting some changes in
	 * membership, etc.
	 * @param consumer
	 */
	void listenEvents(Consumer<ClusterEvent> consumer);

	/**
	 * Returns the current Reveno Cluster View. It is represented by unique nubmer and the list
	 * of current active members which are in current group. If new node(s) comes in/out of the cluster,
	 * new View instance is registered among all rest active nodes, which in turn require new
	 * leadership election process to be started.
	 * @return
	 */
	ClusterView view();
	
}
