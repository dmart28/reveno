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

import org.reveno.atp.api.EventsManager;
import org.reveno.atp.utils.MapUtils;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SuppressWarnings("unchecked")
public class EventHandlersManager implements EventsManager {

	@Override
	public void asyncEventExecutors(int count) {
		close();
		
		asyncListenersExecutor = IntStream.range(0, count)
				.mapToObj(i -> Executors.newSingleThreadExecutor())
				.collect(Collectors.toList());
	}
	
	public ExecutorService asyncEventExecutor() {
		return asyncListenersExecutor.get(rand.nextInt(asyncListenersExecutor.size()));
	}
	
	@Override
	public <E> void asyncEventHandler(Class<E> eventType, BiConsumer<E, EventMetadata> consumer) {
		asyncListeners.get(eventType).add((BiConsumer<Object, EventMetadata>) consumer);
	}

	@Override
	public <E> void eventHandler(Class<E> eventType, BiConsumer<E, EventMetadata> consumer) {
		listeners.get(eventType).add((BiConsumer<Object, EventMetadata>) consumer);
	}

	@Override
	public <E> void removeEventHandler(Class<E> eventType, BiConsumer<E, EventMetadata> consumer) {
		listeners.get(eventType).remove(consumer);
	}
	
	public Set<BiConsumer<Object, EventMetadata>> getEventHandlers(Class<?> eventType) {
		return listeners.get(eventType);
	}
	
	public Set<BiConsumer<Object, EventMetadata>> getAsyncHandlers(Class<?> eventType) {
		return asyncListeners.get(eventType);
	}

	public void close() {
		asyncListenersExecutor.forEach(ExecutorService::shutdown);
	}
	
	protected Random rand = new Random();
	protected Map<Class<?>, Set<BiConsumer<Object, EventMetadata>>> listeners = MapUtils.repositorySet();
	protected Map<Class<?>, Set<BiConsumer<Object, EventMetadata>>> asyncListeners = MapUtils.repositorySet();
	protected List<ExecutorService> asyncListenersExecutor = Collections.singletonList(Executors.newSingleThreadExecutor());
}
