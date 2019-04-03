package org.reveno.atp.core;

import org.reveno.atp.core.api.EventsCommitInfo.Builder;
import org.reveno.atp.core.api.Journaler;
import org.reveno.atp.core.api.serialization.EventsInfoSerializer;
import org.reveno.atp.core.events.EventHandlersManager;
import org.reveno.atp.core.events.EventsContext;

public class EngineEventsContext implements EventsContext {
    private Journaler eventsJournaler;
    private Builder eventsCommitBuilder;
    private EventsInfoSerializer serializer;
    private EventHandlersManager manager;

    @Override
    public Journaler eventsJournaler() {
        return eventsJournaler;
    }

    public EngineEventsContext eventsJournaler(Journaler eventsJournaler) {
        this.eventsJournaler = eventsJournaler;
        return this;
    }

    @Override
    public Builder eventsCommitBuilder() {
        return eventsCommitBuilder;
    }

    public EngineEventsContext eventsCommitBuilder(Builder eventsCommitBuilder) {
        this.eventsCommitBuilder = eventsCommitBuilder;
        return this;
    }

    @Override
    public EventsInfoSerializer serializer() {
        return serializer;
    }

    public EngineEventsContext serializer(EventsInfoSerializer serializer) {
        this.serializer = serializer;
        return this;
    }

    @Override
    public EventHandlersManager manager() {
        return manager;
    }

    public EngineEventsContext manager(EventHandlersManager manager) {
        this.manager = manager;
        return this;
    }
}
