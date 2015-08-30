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

package org.reveno.atp.metrics.meter;

import org.reveno.atp.metrics.Sink;

import java.util.List;

public interface Sinkable {

	/**
	 * Sends the last snapshot of metrics to some list of {@link Sink}.
	 * 
	 * @param sinks pipes into which last metrics will be loaded
	 * @param sync whether to await while it will be safe to catch results ({@value true} make sense only in multithreaded environment)
	 */
	void sendTo(List<Sink> sinks, boolean sync);
	
}
