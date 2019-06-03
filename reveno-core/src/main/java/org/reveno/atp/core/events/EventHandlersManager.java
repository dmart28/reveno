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
    protected Random rand = new Random();
    protected Map<Class<?>, Set<BiConsumer<Object, EventMetadata>>> listeners = MapUtils.repositorySet();
    protected Map<Class<?>, Set<BiConsumer<Object, EventMetadata>>> asyncListeners = MapUtils.repositorySet();
    protected List<ExecutorService> asyncListenersExecutor = Collections.singletonList(Executors.newSingleThreadExecutor());

    @Override
    public void asyncEventExecutors(int count) {
        close();

        asyncListenersExecutor = IntStream.range(0, count)
                .mapToObj(i -> Executors.newSingleThreadExecutor())
                .collect(Collectors.toList());
    }

    public ExecutorService asyncEventExecutor() {
        // TODO when we call this method, we have sequencer, so we can apply
        // simple round-robin here, which will perform faster than random.
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
}
