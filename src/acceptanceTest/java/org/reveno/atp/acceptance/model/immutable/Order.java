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

import static org.reveno.atp.acceptance.model.Immutable.copy;
import static org.reveno.atp.acceptance.model.Immutable.pair;

import java.util.Optional;

public class Order {

	public final long id;
	public final long accountId;
	public final Optional<Long> positionId;
	public final String symbol;
	public final long price;
	public final long size;
	public final OrderStatus status;
	public final OrderType type;
	
	public Order update(OrderStatus status) {
		return copy(this, pair("status", status));
	}
	
	public Order(long id, long accId, Optional<Long> positionId, String symbol,
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
	
	public static enum OrderStatus {
		PENDING, FILLED, CANCELLED
	}
	
	public static enum OrderType {
		MARKET, LIMIT, STOP
	}
	
}
