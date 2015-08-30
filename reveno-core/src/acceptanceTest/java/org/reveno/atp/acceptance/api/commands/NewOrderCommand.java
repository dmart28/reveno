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

package org.reveno.atp.acceptance.api.commands;

import org.reveno.atp.acceptance.model.Order.OrderType;

import java.io.Serializable;
import java.util.Optional;

public class NewOrderCommand implements Serializable {

	public final long accountId;
	public final Long positionId;
	public final String symbol;
	public final long price;
	public final long size;
	public final OrderType orderType;
	
	public NewOrderCommand(long accountId, Long positionId,
			String symbol, long price, long size, OrderType orderType) {
		this.accountId = accountId;
		this.positionId = positionId;
		this.symbol = symbol;
		this.price = price;
		this.size = size;
		this.orderType = orderType;
	}
	
}
