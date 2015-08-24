package org.reveno.atp.acceptance.handlers;

import org.reveno.atp.acceptance.api.transactions.AcceptOrder;
import org.reveno.atp.acceptance.api.transactions.CreateAccount;
import org.reveno.atp.acceptance.api.transactions.Credit;
import org.reveno.atp.acceptance.api.transactions.Debit;
import org.reveno.atp.acceptance.model.Account;
import org.reveno.atp.acceptance.model.Order;
import org.reveno.atp.api.transaction.TransactionContext;

import java.util.Optional;

public class RollbackTransactions {

	public static void rollbackCreateAccount(CreateAccount tx, TransactionContext ctx) {
		ctx.repository().remove(Account.class, tx.id);
	}
	
	public static void rollbackCredit(Credit tx, TransactionContext ctx) {
		Account acc = ctx.repository().get(Account.class, tx.accountId).get();
		acc.addBalance(-tx.amount);
	}
	
	public static void rollbackDebit(Debit tx, TransactionContext ctx) {
		Optional<Account> acc = ctx.repository().get(Account.class, tx.accountId);
		acc.get().addBalance(tx.amount);
	}
	
	public static void rollbackAcceptOrder(AcceptOrder tx, TransactionContext ctx) {
		Account account = ctx.repository().get(Account.class, tx.accountId).get();
		account.removeOrder(tx.id);
		ctx.repository().remove(Order.class, tx.id);
	}
	
}
