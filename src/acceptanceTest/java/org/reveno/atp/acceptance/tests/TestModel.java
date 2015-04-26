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

import org.junit.Assert;
import org.junit.Test;
import org.reveno.atp.acceptance.model.immutable.Account;

public class TestModel {

	@Test
	public void test() {
		Account acc1 = new Account(1, "USD", 500);
		Account acc2 = acc1.increaseBalance(500);
		
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
	}
	
}
