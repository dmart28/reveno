package org.reveno.atp.acceptance.model.mutable;

import org.reveno.atp.acceptance.model.Fill;
import org.reveno.atp.acceptance.model.Order;

public class MutableFill implements Fill {

	private final long id;
	private final long accountId;
	private final long positionId;
	private final long size;
	private final long price;
	private final Order order;
	
	public long id() {
		return id;
	}
	
	public long accountId() {
		return accountId;
	}
	
	public long positionId() {
		return positionId;
	}
	
	public long size() {
		return size;
	}
	
	public long price() {
		return price;
	}
	
	public Order order() {
		return order;
	}
	
	public MutableFill(long id, long accountId, long positionId, long size, long price, Order order) {
		this.id = id;
		this.accountId = accountId;
		this.positionId = positionId;
		this.size = size;
		this.price = price;
		this.order = order;
	}

	@Override
	public boolean isImmutable() {
		return false;
	}
	
	public static final FillFactory FACTORY = new FillFactory() {
		@Override
		public Fill create(long id, long accountId, long positionId, long size,
				long price, Order order) {
			return new MutableFill(id, accountId, positionId, size, price, order);
		}
	};
	
}
