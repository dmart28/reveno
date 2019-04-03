package org.reveno.atp.api.transaction;

import org.reveno.atp.api.domain.WriteableRepository;
import org.reveno.atp.core.api.Destroyable;

/*
 * Allows you to intercept into transaction processing.
 *
 * WARNING: it's some kind of dangerous field, and it should be used with
 * super care, since inappropriate usage might be extremely harmful.
 *
 * Good practice is to use it for some statistics gathering, snapshotting logic, etc.
 *
 */
public interface TransactionInterceptor extends Destroyable {

    void intercept(long transactionId, long time, long flags,
                   WriteableRepository repository, TransactionStage stage);

}
