package org.reveno.atp.acceptance.api.transactions;

import org.reveno.atp.acceptance.model.Order.OrderType;

import java.io.Serializable;

public class AcceptOrder implements Serializable {

	public final long id;
	public final long accountId;
	public final Long positionId;
	public final String symbol;
	public final long price;
	public final long size;
	public final OrderType orderType;
	
	public AcceptOrder(long id, long accountId, Long positionId,
			String symbol, long price, long size, OrderType orderType) {
		this.id = id;
		this.accountId = accountId;
		this.positionId = positionId;
		this.symbol = symbol;
		this.price = price;
		this.size = size;
		this.orderType = orderType;
	}
	
}
