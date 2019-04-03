package org.reveno.atp.core.api;

import org.reveno.atp.api.transaction.EventBus;

public interface RestoreableEventBus extends EventBus {

    RestoreableEventBus currentTransactionId(long transactionId);

    RestoreableEventBus underlyingEventBus(EventBus eventBus);

}
