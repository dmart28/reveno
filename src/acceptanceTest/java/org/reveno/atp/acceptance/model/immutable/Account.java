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

import static org.reveno.atp.acceptance.model.Immutable.sa;
import static org.reveno.atp.acceptance.model.Immutable.sr;
import static org.reveno.atp.acceptance.model.Immutable.copy;
import static org.reveno.atp.acceptance.model.Immutable.pair;

import java.util.HashSet;
import java.util.List;

public class Account {

	public final long id;
	public final String currency;
	public final long balance;
	public final HashSet<Long> orders;
	public final PositionBook positions;
	
	public Account addBalance(long add) {
		return copy(this, pair("balance", balance + add));
	}
	
	public Account addOrder(long orderId) {
		return copy(this, pair("orders", sa(orders, orderId)));
	}
	
	public Account removeOrder(long orderId) {
		return copy(this, pair("orders", sr(orders, orderId)));
	}
	
	public Account addPosition(long id, String symbol, Fill fill) {
		return copy(this, pair("positions", positions.addPosition(id, symbol, fill)));
	}
	
	public Account applyFill(Fill fill) {
		return copy(this, pair("positions", positions.applyFill(fill)));
	}
	
	public Account mergePositions(long toPositionId, List<Long> mergedPositions) {
		return copy(this, pair("positions", positions.merge(toPositionId, mergedPositions)));
	}
	
	public Account exitPosition(long positionId, long pnl) {
		return copy(this, pair("positions", positions.exit(positionId), "balance", balance + pnl));
	}
	
	public Account(long id, String currency, long balance) {
		this(id, currency, balance, new HashSet<>(), new PositionBook());
	}
	
	public Account(long id, String currency, long balance, HashSet<Long> orders, PositionBook positions) {
		this.id = id;
		this.currency = currency;
		this.balance = balance;
		this.orders = orders;
		this.positions = positions;
	}
	
}
