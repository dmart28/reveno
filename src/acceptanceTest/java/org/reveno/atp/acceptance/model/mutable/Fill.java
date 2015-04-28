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

import org.reveno.atp.acceptance.model.immutable.Order;

public class Fill {

	public final long id;
	public final long accountId;
	public final long positionId;
	public final long size;
	public final long price;
	public final Order order;
	
	public Fill(long id, long accountId, long positionId, long size, long price, Order order) {
		this.id = id;
		this.accountId = accountId;
		this.positionId = positionId;
		this.size = size;
		this.price = price;
		this.order = order;
	}
	
}
