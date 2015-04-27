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
import org.reveno.atp.acceptance.model.immutable.Account;
import org.reveno.atp.acceptance.model.immutable.Fill;
import org.reveno.atp.acceptance.model.immutable.Order;
import org.reveno.atp.acceptance.model.immutable.Order.OrderStatus;
import org.reveno.atp.acceptance.model.immutable.Order.OrderType;

public class TestModel {

	@Test
	public void test() {
		Account acc1 = new Account(1, "USD", 500);
		Account acc2 = acc1.addBalance(500);
		
		Assert.assertNotEquals(acc1, acc2);
		Assert.assertEquals(500, acc1.balance);
		Assert.assertEquals(1000, acc2.balance);
		Assert.assertEquals(1, acc2.id);
		Assert.assertEquals("USD", acc2.currency);
		
		Account acc3 = acc2.addOrder(5L);
		
		Assert.assertNotEquals(acc3, acc2);
		Assert.assertEquals(1000, acc3.balance);
		Assert.assertEquals(0, acc2.orders.size());
		Assert.assertEquals(1, acc3.orders.size());
		
		Account acc4 = acc3.removeOrder(5L);
		
		Assert.assertNotEquals(acc4, acc3);
		Assert.assertEquals(1000, acc4.balance);
		Assert.assertEquals(1, acc3.orders.size());
		Assert.assertEquals(0, acc4.orders.size());
		
		Order order = new Order(1, 1, Optional.empty(), "EUR/USD", 134000, 700L, OrderStatus.FILLED, OrderType.MARKET);
		Order order1 =  new Order(2, 1, Optional.empty(), "EUR/USD", 135000, 300L, OrderStatus.FILLED, OrderType.MARKET);
		acc4 = acc4.addPosition(1L, "EUR/USD", new Fill(1L, acc4.id, 1, order.size, order.price, order));
		
		Assert.assertEquals(1, acc4.positions.positions.size());
		Assert.assertEquals(700, acc4.positions.positions.get(1L).sum());
		acc4 = acc4.applyFill(new Fill(2L,acc4.id, 1L, order1.size, order1.price, order1));
		Assert.assertEquals(1000, acc4.positions.positions.get(1L).sum());
		Assert.assertFalse(acc4.positions.positions.get(1L).isComplete());
		
		order = new Order(3, 1, Optional.of(1L), "EUR/USD", 128000, -1000, OrderStatus.FILLED, OrderType.MARKET);
		acc4 = acc4.applyFill(new Fill(3L, acc4.id, 1L, order.size, order.price, order));
		
		Assert.assertTrue(acc4.positions.positions.get(1L).isComplete());
	}
	
}
