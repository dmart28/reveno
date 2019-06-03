package org.reveno.atp.acceptance.handlers;

import org.reveno.atp.acceptance.api.transactions.AcceptOrder;
import org.reveno.atp.acceptance.api.transactions.CreateAccount;
import org.reveno.atp.acceptance.api.transactions.Credit;
import org.reveno.atp.acceptance.api.transactions.Debit;
import org.reveno.atp.acceptance.model.Account;
import org.reveno.atp.acceptance.model.Order;
import org.reveno.atp.api.transaction.TransactionContext;

public class RollbackTransactions {

    public static void rollbackCreateAccount(CreateAccount tx, TransactionContext ctx) {
        ctx.repo().remove(Account.class, tx.id);
    }

    public static void rollbackCredit(Credit tx, TransactionContext ctx) {
        Account acc = ctx.repo().get(Account.class, tx.accountId);
        acc.addBalance(-tx.amount);
    }

    public static void rollbackDebit(Debit tx, TransactionContext ctx) {
        Account acc = ctx.repo().get(Account.class, tx.accountId);
        acc.addBalance(tx.amount);
    }

    public static void rollbackAcceptOrder(AcceptOrder tx, TransactionContext ctx) {
        Account account = ctx.repo().get(Account.class, tx.accountId);
        account.removeOrder(tx.id);
        ctx.repo().remove(Order.class, tx.id);
    }

}
