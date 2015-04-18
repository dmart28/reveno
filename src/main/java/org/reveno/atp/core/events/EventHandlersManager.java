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

package org.reveno.atp.core.events;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.reveno.atp.api.EventsManager;
import org.reveno.atp.utils.MapUtils;

@SuppressWarnings("unchecked")
public class EventHandlersManager implements EventsManager {

	@Override
	public void asyncEventExecutor(ExecutorService executor) {
		if (this.asyncListenersExecutor != null)
			this.asyncListenersExecutor.shutdown();
		this.asyncListenersExecutor = executor;
	}
	
	@Override
	public <E> void asyncEventHandler(Class<E> eventType, Consumer<E> consumer) {
		asyncListeners.get(eventType).add((Consumer<Object>) consumer);
	}

	@Override
	public <E> void eventHandler(Class<E> eventType, Consumer<E> consumer) {
		listeners.get(eventType).add((Consumer<Object>) consumer);
	}

	@Override
	public <E> void removeEventHandler(Class<E> eventType, Consumer<E> consumer) {
		listeners.get(eventType).remove(consumer);
	}
	
	public Set<Consumer<Object>> getEventHandlers(Class<?> eventType) {
		return listeners.get(eventType);
	}
	
	public Set<Consumer<Object>> getAsyncHandlers(Class<?> eventType) {
		return asyncListeners.get(eventType);
	}

	
	protected Map<Class<?>, Set<Consumer<Object>>> listeners = MapUtils.repositorySet();
	protected Map<Class<?>, Set<Consumer<Object>>> asyncListeners = MapUtils.repositorySet();
	protected ExecutorService asyncListenersExecutor = Executors.newSingleThreadExecutor();
}
