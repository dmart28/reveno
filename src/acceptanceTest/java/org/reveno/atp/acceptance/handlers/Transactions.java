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

import java.util.Optional;

import org.reveno.atp.acceptance.api.events.AccountCreatedEvent;
import org.reveno.atp.acceptance.api.events.OrderCreatedEvent;
import org.reveno.atp.acceptance.api.transactions.AcceptOrder;
import org.reveno.atp.acceptance.api.transactions.CreateAccount;
import org.reveno.atp.acceptance.api.transactions.Credit;
import org.reveno.atp.acceptance.api.transactions.Debit;
import org.reveno.atp.acceptance.model.Account;
import org.reveno.atp.acceptance.model.Account.AccountFactory;
import org.reveno.atp.acceptance.model.Order;
import org.reveno.atp.acceptance.model.Order.OrderFactory;
import org.reveno.atp.acceptance.model.Order.OrderStatus;
import org.reveno.atp.api.transaction.TransactionContext;

public abstract class Transactions {

	public static void createAccount(CreateAccount tx, TransactionContext ctx) {
		ctx.repository().store(tx.id, Account.class, accountFactory.create(tx.id, tx.currency, 0L));
		ctx.eventBus().publishEvent(new AccountCreatedEvent(tx.id));
	}
	
	public static void credit(Credit tx, TransactionContext ctx) {
		Account acc = ctx.repository().get(Account.class, tx.accountId).get();
		if (acc.isImmutable()) 
			ctx.repository().store(tx.accountId, Account.class, acc.addBalance(tx.amount));
		else
			acc.addBalance(tx.amount);
	}
	
	public static void debit(Debit tx, TransactionContext ctx) {
		Optional<Account> acc = ctx.repository().get(Account.class, tx.accountId);
		if (!acc.isPresent())
			throw new RuntimeException("Can't find account with id=" + tx.accountId);
		
		if (acc.get().balance() < tx.amount)
			throw new IllegalStateException("The account balance is less than debit amount!");
		
		if (acc.get().isImmutable()) 
			ctx.repository().store(tx.accountId, Account.class, acc.get().addBalance(-tx.amount));
		else
			acc.get().addBalance(-tx.amount);
	}
	
	public static void acceptOrder(AcceptOrder tx, TransactionContext ctx) {
		Account account = ctx.repository().get(Account.class, tx.accountId).get();
		if (account.isImmutable())
			ctx.repository().store(tx.accountId, Account.class, account.addOrder(tx.id));
		else 
			account.addOrder(tx.id);
		
		ctx.repository().store(tx.id, Order.class, orderFactory.create(tx.id, tx.accountId, tx.positionId, tx.symbol,
				tx.price, tx.size, System.currentTimeMillis(), OrderStatus.PENDING, tx.orderType));
		ctx.eventBus().publishEvent(new OrderCreatedEvent(tx.id, tx.accountId));
	}
	
	public static AccountFactory accountFactory;
	public static OrderFactory orderFactory;
	
}
