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

package org.reveno.atp.core;

import org.reveno.atp.core.api.EventsCommitInfo.Builder;
import org.reveno.atp.core.api.Journaler;
import org.reveno.atp.core.api.serialization.EventsInfoSerializer;
import org.reveno.atp.core.events.EventHandlersManager;
import org.reveno.atp.core.events.EventsContext;

public class EngineEventsContext implements EventsContext {

	private Journaler eventsJournaler;
	@Override
	public Journaler eventsJournaler() {
		return eventsJournaler;
	}
	public EngineEventsContext eventsJournaler(Journaler eventsJournaler) {
		this.eventsJournaler = eventsJournaler;
		return this;
	}

	private Builder eventsCommitBuilder;
	@Override
	public Builder eventsCommitBuilder() {
		return eventsCommitBuilder;
	}
	public EngineEventsContext eventsCommitBuilder(Builder eventsCommitBuilder) {
		this.eventsCommitBuilder = eventsCommitBuilder;
		return this;
	}

	private EventsInfoSerializer serializer;
	@Override
	public EventsInfoSerializer serializer() {
		return serializer;
	}
	public EngineEventsContext serializer(EventsInfoSerializer serializer) {
		this.serializer = serializer;
		return this;
	}

	private EventHandlersManager manager;
	@Override
	public EventHandlersManager manager() {
		return manager;
	}
	public EngineEventsContext manager(EventHandlersManager manager) {
		this.manager = manager;
		return this;
	}

}
