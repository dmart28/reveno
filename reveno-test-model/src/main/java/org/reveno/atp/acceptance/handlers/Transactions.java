package org.reveno.atp.acceptance.handlers;

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

import java.util.Optional;

public abstract class Transactions {

	public static void createAccount(CreateAccount tx, TransactionContext ctx) {
		ctx.repo().store(tx.id, Account.class, accountFactory.create(tx.id, tx.currency, 0L));
		ctx.eventBus().publishEvent(new AccountCreatedEvent(tx.id));
	}
	
	public static void credit(Credit tx, TransactionContext ctx) {
		Account acc = ctx.repo().get(Account.class, tx.accountId);
		if (acc.isImmutable())
			ctx.repo().store(tx.accountId, Account.class, acc.addBalance(tx.amount));
		else
			acc.addBalance(tx.amount);
	}
	
	public static void debit(Debit tx, TransactionContext ctx) {
		Account acc = ctx.repo().get(Account.class, tx.accountId);
		
		if (acc.isImmutable())
			ctx.repo().store(tx.accountId, Account.class, acc.addBalance(-tx.amount));
		else
			acc.addBalance(-tx.amount);
	}
	
	public static void acceptOrder(AcceptOrder tx, TransactionContext ctx) {
		Account account = ctx.repo().get(Account.class, tx.accountId);
		if (account.isImmutable())
			ctx.repo().store(tx.accountId, Account.class, account.addOrder(tx.id));
		else
			account.addOrder(tx.id);
		
		ctx.repo().store(tx.id, Order.class, orderFactory.create(tx.id, tx.accountId, Optional.ofNullable(tx.positionId), tx.symbol,
				tx.price, tx.size, System.currentTimeMillis(), OrderStatus.PENDING, tx.orderType));
		ctx.eventBus().publishEvent(new OrderCreatedEvent(tx.id, tx.accountId));
	}
	
	public static AccountFactory accountFactory;
	public static OrderFactory orderFactory;
	
}
