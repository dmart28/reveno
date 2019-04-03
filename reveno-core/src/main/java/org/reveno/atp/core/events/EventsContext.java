package org.reveno.atp.core.events;

import org.reveno.atp.core.api.EventsCommitInfo;
import org.reveno.atp.core.api.Journaler;
import org.reveno.atp.core.api.serialization.EventsInfoSerializer;

public interface EventsContext {

    Journaler eventsJournaler();

    EventsCommitInfo.Builder eventsCommitBuilder();

    EventsInfoSerializer serializer();

    EventHandlersManager manager();

}
