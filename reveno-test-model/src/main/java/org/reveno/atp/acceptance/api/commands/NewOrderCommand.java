package org.reveno.atp.acceptance.api.commands;

import org.reveno.atp.acceptance.model.Order.OrderType;

import java.io.Serializable;

public class NewOrderCommand implements Serializable {

	public final long accountId;
	public final Long positionId;
	public final String symbol;
	public final long price;
	public final long size;
	public final OrderType orderType;
	
	public NewOrderCommand(long accountId, Long positionId,
			String symbol, long price, long size, OrderType orderType) {
		this.accountId = accountId;
		this.positionId = positionId;
		this.symbol = symbol;
		this.price = price;
		this.size = size;
		this.orderType = orderType;
	}
	
}
