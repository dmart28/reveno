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

import org.reveno.atp.acceptance.model.Order;

import java.util.Optional;

public class MutableOrder implements Order {

	private final long id;
	private final long accountId;
	private final Long positionId;
	private final String symbol;
	private final long price;
	private final long size;
	private final long time;
	private final OrderType type;
	private OrderStatus status;
	
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
	
	public OrderType orderType() {
		return type;
	}
	
	public OrderStatus orderStatus() {
		return status;
	}
	
	public Order update(OrderStatus newStatus) {
		this.status = newStatus;
		return this;
	}
	
	public MutableOrder(long id, long accId, Optional<Long> positionId, String symbol,
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

	@Override
	public boolean isImmutable() {
		return false;
	}
	
	public static final OrderFactory FACTORY = new OrderFactory() {
		@Override
		public Order create(long id, long accId, Optional<Long> positionId,
				String symbol, long price, long size, long time, OrderStatus status,
				OrderType type) {
			return new MutableOrder(id, accId, positionId, symbol, price, size, time, status, type);
		}
	};
	
}
