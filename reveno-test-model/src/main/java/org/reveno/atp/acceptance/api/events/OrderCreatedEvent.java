package org.reveno.atp.acceptance.api.events;

public class OrderCreatedEvent {

    public final long orderId;
    public final long accountId;

    public OrderCreatedEvent(long orderId, long accountId) {
        this.orderId = orderId;
        this.accountId = accountId;
    }

}
