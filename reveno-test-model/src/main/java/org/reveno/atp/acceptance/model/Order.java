package org.reveno.atp.acceptance.model;

import java.util.Optional;

public interface Order extends Changeable {

	long id();
	
	long accountId();
	
	Optional<Long> positionId();
	
	String symbol();
	
	long price();
	
	long size();
	
	long time();
	
	OrderStatus orderStatus();
	
	OrderType orderType();
	
	Order update(OrderStatus orderStatus);
	
	public static enum OrderStatus {
		PENDING, FILLED, CANCELLED
	}
	
	public static enum OrderType {
		MARKET, LIMIT, STOP
	}
	
	
	public interface OrderFactory {
		Order create(long id, long accId, Optional<Long> positionId, String symbol,
					 long price, long size, long time, OrderStatus status, OrderType type);
	}
	
}
