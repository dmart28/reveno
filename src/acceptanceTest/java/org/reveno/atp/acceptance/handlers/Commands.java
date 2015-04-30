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

package org.reveno.atp.acceptance.handlers;

import org.reveno.atp.acceptance.api.commands.CreateNewAccountCommand;
import org.reveno.atp.acceptance.api.transactions.CreateAccount;
import org.reveno.atp.acceptance.api.transactions.Credit;
import org.reveno.atp.acceptance.model.Account;
import org.reveno.atp.api.commands.CommandContext;

public abstract class Commands {

	public static Long createAccount(CreateNewAccountCommand cmd, CommandContext ctx) {
		long accountId = ctx.id(Account.class);
		ctx.executeTransaction(new CreateAccount(accountId, cmd.currency));
		
		if (cmd.balance > 0)
			ctx.executeTransaction(new Credit(accountId, cmd.balance));
		
		return accountId;
	}
	
}
