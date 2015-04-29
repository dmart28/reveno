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

package org.reveno.atp.acceptance.tests;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.reveno.atp.acceptance.model.Order.OrderStatus;
import org.reveno.atp.acceptance.model.Order.OrderType;
import org.reveno.atp.acceptance.model.immutable.ImmutableAccount;
import org.reveno.atp.acceptance.model.immutable.ImmutableFill;
import org.reveno.atp.acceptance.model.immutable.ImmutableOrder;

public class TestModel {

	@Test
	public void test() {
		ImmutableAccount acc1 = new ImmutableAccount(1, "USD", 500_000);
		ImmutableAccount acc2 = acc1.addBalance(500_000);
		
		Assert.assertNotEquals(acc1, acc2);
		Assert.assertEquals(500_000, acc1.balance());
		Assert.assertEquals(1000_000, acc2.balance());
		Assert.assertEquals(1, acc2.id());
		Assert.assertEquals("USD", acc2.currency());
		
		ImmutableAccount acc3 = acc2.addOrder(5L);
		
		Assert.assertNotEquals(acc3, acc2);
		Assert.assertEquals(1000_000, acc3.balance());
		Assert.assertEquals(0, acc2.orders().size());
		Assert.assertEquals(1, acc3.orders().size());
		
		ImmutableAccount acc4 = acc3.removeOrder(5L);
		
		Assert.assertNotEquals(acc4, acc3);
		Assert.assertEquals(1000_000, acc4.balance());
		Assert.assertEquals(1, acc3.orders().size());
		Assert.assertEquals(0, acc4.orders().size());
		
		ImmutableOrder order = new ImmutableOrder(1, 1, Optional.empty(), "EUR/USD", 134000, 700L, OrderStatus.FILLED, OrderType.MARKET);
		ImmutableOrder order1 =  new ImmutableOrder(2, 1, Optional.empty(), "EUR/USD", 135000, 300L, OrderStatus.FILLED, OrderType.MARKET);
		acc4 = acc4.addPosition(1L, "EUR/USD", new ImmutableFill(1L, acc4.id(), 1, order.size(), order.price(), order));
		
		Assert.assertEquals(1, acc4.positions().positions().size());
		Assert.assertEquals(700, acc4.positions().positions().get(1L).sum());
		acc4 = acc4.applyFill(new ImmutableFill(2L,acc4.id(), 1L, order1.size(), order1.price(), order1));
		Assert.assertEquals(1000, acc4.positions().positions().get(1L).sum());
		Assert.assertFalse(acc4.positions().positions().get(1L).isComplete());
		
		ImmutableOrder order2 = new ImmutableOrder(3, 1, Optional.of(1L), "EUR/USD", 128000, -1000, OrderStatus.FILLED, OrderType.MARKET);
		acc4 = acc4.applyFill(new ImmutableFill(3L, acc4.id(), 1L, order2.size(), order2.price(), order2));
		
		Assert.assertTrue(acc4.positions().positions().get(1L).isComplete());
		
		acc4 = acc4.exitPosition(1L, (long)((order.price() * (order.size() / 1000f)) + (order1.price() * (order1.size() / 1000f)))
				- order2.price());
		Assert.assertEquals(0, acc4.positions().positions().size());
		Assert.assertEquals(1000_000 + 6300, acc4.balance());
	}
	
}
