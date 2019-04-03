package org.reveno.atp.core.engine;

import org.reveno.atp.core.JournalsManager;
import org.reveno.atp.core.RevenoConfiguration;
import org.reveno.atp.core.api.*;
import org.reveno.atp.core.engine.components.CommandsManager;
import org.reveno.atp.core.engine.components.SerializersChain;
import org.reveno.atp.core.engine.components.TransactionsManager;
import org.reveno.atp.core.events.EventPublisher;
import org.reveno.atp.core.snapshots.SnapshottersManager;
import org.reveno.atp.core.views.ViewsProcessor;

public interface WorkflowContext {

    RevenoConfiguration configuration();

    ClassLoader classLoader();

    IdGenerator idGenerator();

    SerializersChain serializer();

    TxRepository repository();

    ViewsProcessor viewsProcessor();

    FailoverManager failoverManager();

    TransactionsManager transactionsManager();

    CommandsManager commandsManager();

    InterceptorCollection interceptorCollection();


    EventPublisher eventPublisher();


    TransactionCommitInfo.Builder transactionCommitBuilder();

    Journaler transactionJournaler();

    JournalsManager journalsManager();

    SnapshottersManager snapshotsManager();

}
