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

import java.util.Optional;

import org.reveno.atp.acceptance.model.Order;

public class ImmutableOrder implements Order {

	private final long id;
	private final long accountId;
	private final Long positionId;
	private final String symbol;
	private final long price;
	private final long size;
	private final long time;
	private final OrderStatus status;
	private final OrderType type;
	
	public long id() {
		return id;
	}
	
	public long accountId() {
		return accountId;
	}
	
	public Optional<Long> positionId() {
		return Optional.ofNullable(positionId);
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
	
	public long time() {
		return time;
	}
	
	public OrderStatus orderStatus() {
		return status;
	}
	
	public OrderType orderType() {
		return type;
	}
	
	public ImmutableOrder update(OrderStatus status) {
		return new ImmutableOrder(this.id, this.accountId, this.positionId(), this.symbol, this.price, this.size, this.time, status, this.type);
	}
	
	public ImmutableOrder(long id, long accId, Optional<Long> positionId, String symbol,
			long price, long size, long time, OrderStatus status, OrderType type) {
		this.id = id;
		this.accountId = accId;
		this.positionId = positionId.orElse(null);
		this.symbol = symbol;
		this.price = price;
		this.size = size;
		this.time = time;
		this.status = status;
		this.type = type;
	}
	
	public static final OrderFactory FACTORY = new OrderFactory() {
		@Override
		public Order create(long id, long accId, Optional<Long> positionId,
				String symbol, long price, long size, long time, OrderStatus status,
				OrderType type) {
			return new ImmutableOrder(id, accId, positionId, symbol, price, size, time, status, type);
		}
	};

	@Override
	public boolean isImmutable() {
		return true;
	}
	
}
