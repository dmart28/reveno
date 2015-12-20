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

package org.reveno.atp.acceptance.model.mutable;

import org.reveno.atp.acceptance.model.Account;
import org.reveno.atp.acceptance.model.Fill;
import org.reveno.atp.acceptance.model.PositionBook;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MutableAccount implements Account {

	private final long id;
	private final String currency;
	private long balance;
	private HashSet<Long> orders = new HashSet<>();
	private MutablePositionBook positions = new MutablePositionBook();
	
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
		return orders;
	}
	
	public PositionBook positions() {
		return positions;
	}
	
	public MutableAccount addBalance(long add) {
		this.balance += add;
		return this;
	}
	
	public MutableAccount addOrder(long orderId) {
		orders.add(orderId);
		return this;
	}
	
	public MutableAccount removeOrder(long orderId) {
		orders.remove(orderId);
		return this;
	}
	
	public Account addPosition(long id, String symbol, Fill fill) {
		positions.addPosition(id, symbol, fill);
		return this;
	}
	
	public Account applyFill(Fill fill) {
		positions.applyFill(fill);
		return null;
	}
	
	public Account mergePositions(long toPositionId, List<Long> mergedPositions) {
		positions.merge(toPositionId, mergedPositions);
		return this;
	}
	
	public Account exitPosition(long positionId, long pnl) {
		positions.exit(positionId);
		addBalance(pnl);
		return this;
	}
	
	public MutableAccount(long id, String currency, long balance) {
		this.id = id;
		this.currency = currency;
		this.balance = balance;
	}

	@Override
	public boolean isImmutable() {
		return false;
	}
	
	public static final AccountFactory FACTORY = MutableAccount::new;
	
}
