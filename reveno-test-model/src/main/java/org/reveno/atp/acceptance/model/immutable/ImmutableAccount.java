package org.reveno.atp.acceptance.model.immutable;

import org.reveno.atp.acceptance.model.Account;
import org.reveno.atp.acceptance.model.Fill;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ImmutableAccount implements Account {

	private final long id;
	private final String currency;
	private final long balance;
	private final HashSet<Long> orders;
	private final ImmutablePositionBook positions;
	
	public long id() {
		return id;
	}
	
	public String currency() {
		return currency;
	}
	
	public long balance() {
		return balance;
	}
	
	public Set<Long> orders() {
		return Collections.unmodifiableSet(orders);
	}
	
	public ImmutablePositionBook positions() {
		return positions;
	}
	
	public ImmutableAccount addBalance(long add) {
		return new ImmutableAccount(this.id, this.currency, this.balance + add, this.orders, this.positions);
	}
	
	public ImmutableAccount addOrder(long orderId) {
		HashSet<Long> newOrders = new HashSet<>(orders);
		newOrders.add(orderId);
		return new ImmutableAccount(this.id, this.currency, this.balance, newOrders, this.positions); 
	}
	
	public ImmutableAccount removeOrder(long orderId) {
		HashSet<Long> newOrders = new HashSet<>(orders);
		newOrders.remove(orderId);
		return new ImmutableAccount(this.id, this.currency, this.balance, newOrders, this.positions); 
	}
	
	public ImmutableAccount addPosition(long id, String symbol, Fill fill) {
		return new ImmutableAccount(this.id, this.currency, this.balance, this.orders, positions.addPosition(id, symbol, fill));
	}
	
	public ImmutableAccount applyFill(Fill fill) {
		return new ImmutableAccount(this.id, this.currency, this.balance, this.orders, positions.applyFill(fill));
	}
	
	public ImmutableAccount mergePositions(long toPositionId, List<Long> mergedPositions) {
		return new ImmutableAccount(this.id, this.currency, this.balance, this.orders, positions.merge(toPositionId, mergedPositions));
	}
	
	public ImmutableAccount exitPosition(long positionId, long pnl) {
		return new ImmutableAccount(this.id, this.currency, this.balance + pnl, this.orders, positions.exit(positionId));
	}
	
	public ImmutableAccount(long id, String currency, long balance) {
		this(id, currency, balance, new HashSet<>(), new ImmutablePositionBook());
	}
	
	public ImmutableAccount(long id, String currency, long balance, HashSet<Long> orders, ImmutablePositionBook positions) {
		this.id = id;
		this.currency = currency;
		this.balance = balance;
		this.orders = orders;
		this.positions = positions;
	}
	
	public static final AccountFactory FACTORY = ImmutableAccount::new;

	@Override
	public boolean isImmutable() {
		return true;
	}
	
}
