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

package org.reveno.atp.acceptance.views;

import org.reveno.atp.acceptance.model.Order.OrderStatus;
import org.reveno.atp.acceptance.model.Order.OrderType;
import org.reveno.atp.api.query.QueryManager;

import java.util.Optional;

public class OrderView extends ViewBase {

	public final long id;
	
	private final long accountId;
	public AccountView account() {
		return query.find(AccountView.class, accountId);
	}
	
	public final long size;
	public final long price;
	public final long time;
	public final String symbol;
	public final OrderStatus status;
	public final OrderType type;
	
	private final Optional<Long> positionId;
	public Optional<PositionView> position() {
		return positionId.isPresent() ? query.findO(PositionView.class, positionId.get()) : Optional.empty();
	}
	
	public OrderView(long id, long accountId, long size, long price, long time, Optional<Long> positionId,
			String symbol, OrderStatus status, OrderType type, QueryManager query) {
		this.id = id;
		this.accountId = accountId;
		this.size = size;
		this.price = price;
		this.time = time;
		this.symbol = symbol;
		this.status = status;
		this.type = type;
		this.positionId = positionId;
		this.query = query;
	}
	
}
