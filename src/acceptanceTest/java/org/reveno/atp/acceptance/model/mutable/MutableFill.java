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

import org.reveno.atp.acceptance.model.Fill;
import org.reveno.atp.acceptance.model.Order;

public class MutableFill implements Fill {

	private final long id;
	private final long accountId;
	private final long positionId;
	private final long size;
	private final long price;
	private final Order order;
	
	public long id() {
		return id;
	}
	
	public long accountId() {
		return accountId;
	}
	
	public long positionId() {
		return positionId;
	}
	
	public long size() {
		return size;
	}
	
	public long price() {
		return price;
	}
	
	public Order order() {
		return order;
	}
	
	public MutableFill(long id, long accountId, long positionId, long size, long price, Order order) {
		this.id = id;
		this.accountId = accountId;
		this.positionId = positionId;
		this.size = size;
		this.price = price;
		this.order = order;
	}

	@Override
	public boolean isImmutable() {
		return false;
	}
	
	public static final FillFactory FACTORY = new FillFactory() {
		@Override
		public Fill create(long id, long accountId, long positionId, long size,
				long price, Order order) {
			return new MutableFill(id, accountId, positionId, size, price, order);
		}
	};
	
}
