package org.reveno.atp.core.api;

import org.reveno.atp.api.domain.WriteableRepository;

public interface TxRepository extends WriteableRepository {

    void begin();

    void commit();

    void rollback();

}
