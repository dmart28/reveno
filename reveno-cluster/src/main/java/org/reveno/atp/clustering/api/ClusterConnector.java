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

import org.reveno.atp.clustering.api.message.Message;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;

public interface ClusterConnector {

	CompletableFuture<Boolean> send(List<Address> dest, Message message);

	CompletableFuture<Boolean> send(List<Address> dest, Message message, Set<Flag> flags);


	<T extends Message> void receive(int type, Consumer<T> consumer);

	<T extends Message> void receive(int type, Predicate<T> filter, Consumer<T> consumer);

	<T extends Message> void unsubscribe(int type, Consumer<T> consumer);


	default Set<Flag> rsvp() {
		HashSet<Flag> flags = new HashSet<>();
		flags.add(Flag.RSVP);
		return flags;
	}

	default Set<Flag> oob() {
		HashSet<Flag> flags = new HashSet<>();
		flags.add(Flag.OUT_OF_BOUND);
		return flags;
	}

}
