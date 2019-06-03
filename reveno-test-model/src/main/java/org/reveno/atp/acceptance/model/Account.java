package org.reveno.atp.acceptance.model;

import java.util.List;
import java.util.Set;

public interface Account extends Changeable {

    long id();

    String currency();

    long balance();

    Set<Long> orders();

    PositionBook positions();

    Account addBalance(long add);

    Account addOrder(long orderId);

    Account removeOrder(long orderId);

    Account addPosition(long id, String symbol, Fill fill);

    Account applyFill(Fill fill);

    Account mergePositions(long toPositionId, List<Long> mergedPositions);

    Account exitPosition(long positionId, long pnl);


    public interface AccountFactory {
        Account create(long id, String currency, long balance);
    }

}
