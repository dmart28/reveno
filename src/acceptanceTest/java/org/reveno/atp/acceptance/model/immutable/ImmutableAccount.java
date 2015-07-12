/** 
 *  Copyright (c) 2015 The original author or authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.reveno.atp.acceptance.model.immutable;

import static org.reveno.atp.acceptance.utils.Immutable.copy;
import static org.reveno.atp.acceptance.utils.Immutable.pair;
import static org.reveno.atp.acceptance.utils.Immutable.sa;
import static org.reveno.atp.acceptance.utils.Immutable.sr;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.reveno.atp.acceptance.model.Account;
import org.reveno.atp.acceptance.model.Fill;

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
		return copy(this, pair("orders", sa(orders, orderId)));
	}
	
	public ImmutableAccount removeOrder(long orderId) {
		return copy(this, pair("orders", sr(orders, orderId)));
	}
	
	public ImmutableAccount addPosition(long id, String symbol, Fill fill) {
		return copy(this, pair("positions", positions.addPosition(id, symbol, fill)));
	}
	
	public ImmutableAccount applyFill(Fill fill) {
		return copy(this, pair("positions", positions.applyFill(fill)));
	}
	
	public ImmutableAccount mergePositions(long toPositionId, List<Long> mergedPositions) {
		return copy(this, pair("positions", positions.merge(toPositionId, mergedPositions)));
	}
	
	public ImmutableAccount exitPosition(long positionId, long pnl) {
		return copy(this, pair("positions", positions.exit(positionId), "balance", balance + pnl));
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
	
	public static final AccountFactory FACTORY = new AccountFactory() {
		@Override
		public Account create(long id, String currency, long balance) {
			return new ImmutableAccount(id, currency, balance);
		}
	};

	@Override
	public boolean isImmutable() {
		return true;
	}
	
}
