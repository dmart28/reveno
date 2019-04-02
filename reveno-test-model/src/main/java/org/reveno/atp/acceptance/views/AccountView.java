package org.reveno.atp.acceptance.views;

import org.reveno.atp.api.query.QueryManager;

import java.util.Set;
import java.util.stream.Collectors;

public class AccountView extends ViewBase {
	public final long accountId;
	public final String currency;
	public long balance;
	
	// lazy evaluation here
	public Set<Long> orders;
	public Set<OrderView> orders() {
		return orders.stream().flatMap(o -> sops(query.findO(OrderView.class, o))).collect(Collectors.toSet());
	}
	
	public Set<PositionView> positions;
	
	public AccountView(long accountId, String currency, long balance, Set<Long> orders, QueryManager query) {
		this.accountId = accountId;
		this.currency = currency;
		this.balance = balance;
		this.orders = orders;
		this.query = query;
	}
	
}
