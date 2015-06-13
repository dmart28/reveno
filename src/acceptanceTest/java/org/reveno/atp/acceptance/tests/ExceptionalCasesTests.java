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

import java.io.File;
import java.io.RandomAccessFile;

import org.junit.Assert;
import org.junit.Test;
import org.reveno.atp.acceptance.api.events.AccountCreatedEvent;
import org.reveno.atp.acceptance.api.events.OrderCreatedEvent;
import org.reveno.atp.api.Reveno;

public class ExceptionalCasesTests extends RevenoBaseTest {

	@Test
	public void testEventsCommitsFileCorrupted() throws Exception {
		Reveno reveno = createEngine();
		reveno.startup();
		
		generateAndSendCommands(reveno, 5_000);
	
		reveno.shutdown();
		
		eraseRandomBuffer(findFirstFile("evn"));
		
		reveno = createEngine();
		Waiter accountCreatedEvent = listenFor(reveno, AccountCreatedEvent.class, Integer.MAX_VALUE);
		Waiter orderCreatedEvent = listenFor(reveno, OrderCreatedEvent.class, Integer.MAX_VALUE);
		reveno.startup();
		
		Assert.assertFalse(accountCreatedEvent.isArrived());
		Assert.assertFalse(orderCreatedEvent.isArrived());
		Assert.assertTrue(accountCreatedEvent.getCount() < Integer.MAX_VALUE);
		Assert.assertTrue(orderCreatedEvent.getCount() < Integer.MAX_VALUE);
		
		reveno.shutdown();
		
		reveno = createEngine();
		accountCreatedEvent = listenFor(reveno, AccountCreatedEvent.class, 1);
		orderCreatedEvent = listenFor(reveno, OrderCreatedEvent.class, 1);
		reveno.startup();
		
		Assert.assertFalse(accountCreatedEvent.isArrived());
		Assert.assertFalse(orderCreatedEvent.isArrived());
		Assert.assertEquals(1, accountCreatedEvent.getCount());
		Assert.assertEquals(1, orderCreatedEvent.getCount());
		
		reveno.shutdown();
	}
	
	
	protected void eraseRandomBuffer(File file) throws Exception {
		try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
			raf.setLength((long)((raf.length() * (Math.random() * Math.random()))));
		}
	}
	
}
