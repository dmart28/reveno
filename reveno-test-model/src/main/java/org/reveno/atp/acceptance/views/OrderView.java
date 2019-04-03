package org.reveno.atp.acceptance.views;

import org.reveno.atp.acceptance.model.Order.OrderStatus;
import org.reveno.atp.acceptance.model.Order.OrderType;
import org.reveno.atp.api.query.QueryManager;

import java.util.Optional;

public class OrderView extends ViewBase {

    public final long id;
    public final long size;
    public final long price;
    public final long time;
    public final String symbol;
    public final OrderStatus status;
    public final OrderType type;
    private final long accountId;
    private final Optional<Long> positionId;

    public OrderView(long id, long accountId, long size, long price, long time, Optional<Long> positionId,
                     String symbol, OrderStatus status, OrderType type, QueryManager query) {
        this.id = id;
        this.accountId = accountId;
        this.size = size;
        this.price = price;
        this.time = time;
        this.symbol = symbol;
        this.status = status;
        this.type = type;
        this.positionId = positionId;
        this.query = query;
    }

    public AccountView account() {
        return query.find(AccountView.class, accountId);
    }

    public Optional<PositionView> position() {
        return positionId.isPresent() ? query.findO(PositionView.class, positionId.get()) : Optional.empty();
    }

}
