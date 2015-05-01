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

import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Test;
import org.reveno.atp.acceptance.api.commands.CreateNewAccountCommand;
import org.reveno.atp.acceptance.api.events.AccountCreatedEvent;
import org.reveno.atp.acceptance.views.AccountView;
import org.reveno.atp.api.Reveno;

public class Tests extends RevenoBaseTest {
	
	@Test 
	public void testBasic() throws InterruptedException, ExecutionException {
		Reveno reveno = createEngine();
		reveno.startup();
		
		Waiter w = listenFor(reveno, AccountCreatedEvent.class);
		long accountId = sendCommandSync(reveno, new CreateNewAccountCommand("RUB", 1000L));
		AccountView view = reveno.query().find(AccountView.class, accountId).get();
		Assert.assertTrue(w.waitFor());
		
		Assert.assertEquals(accountId, view.accountId);
		Assert.assertEquals("RUB", view.currency);
		Assert.assertEquals(1000L, view.balance);
		
		reveno.shutdown();
	}
	
}
