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

import java.util.Optional;

import org.reveno.atp.acceptance.model.Order;

public class ImmutableOrder implements Order {

	private final long id;
	private final long accountId;
	private final Optional<Long> positionId;
	private final String symbol;
	private final long price;
	private final long size;
	private final OrderStatus status;
	private final OrderType type;
	
	public long id() {
		return id;
	}
	
	public long accountId() {
		return accountId;
	}
	
	public Optional<Long> positionId() {
		return positionId;
	}
	
	public String symbol() {
		return symbol;
	}
	
	public long price() {
		return price;
	}
	
	public long size() {
		return size;
	}
	
	public OrderStatus orderStatus() {
		return status;
	}
	
	public OrderType orderType() {
		return type;
	}
	
	public ImmutableOrder update(OrderStatus status) {
		return copy(this, pair("status", status));
	}
	
	public ImmutableOrder(long id, long accId, Optional<Long> positionId, String symbol,
			long price, long size, OrderStatus status, OrderType type) {
		this.id = id;
		this.accountId = accId;
		this.positionId = positionId;
		this.symbol = symbol;
		this.price = price;
		this.size = size;
		this.status = status;
		this.type = type;
	}
	
	public static final OrderFactory FACTORY = new OrderFactory() {
		@Override
		public Order create(long id, long accId, Optional<Long> positionId,
				String symbol, long price, long size, OrderStatus status,
				OrderType type) {
			return new ImmutableOrder(id, accId, positionId, symbol, price, size, status, type);
		}
	};

	@Override
	public boolean isImmutable() {
		return true;
	}
	
}
