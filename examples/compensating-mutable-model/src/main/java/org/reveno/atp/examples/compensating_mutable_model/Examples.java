package org.reveno.atp.examples.compensating_mutable_model;

import org.reveno.atp.api.Configuration;
import org.reveno.atp.api.Reveno;
import org.reveno.atp.core.Engine;
import org.reveno.atp.utils.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Examples {

    protected static final Logger LOG = LoggerFactory.getLogger(Examples.class);

    public static void main(String[] args) throws Exception {
        Reveno reveno = new Engine(args[0]);
        reveno.config().mutableModel();
        reveno.config().mutableModelFailover(Configuration.MutableModelFailover.COMPENSATING_ACTIONS);
        reveno.domain().transaction("createAccount",
                (t,c) -> c.repo().store(t.id(), new Account(t.id(), t.longArg("balance"))))
                .uniqueIdFor(Account.class)
                .conditionalCommand((cmd, c) -> cmd.longArg("balance") >= 0l).command();

        // we use same AddToBalance class as command and tx action for simplicity here
        // as we don't need any special logic in commands rather than just call tx action
        reveno.domain().command(AddToBalance.class, (c, ctx) -> ctx.executeTransaction(c));
        reveno.domain().transactionWithCompensatingAction(AddToBalance.class, (t, ctx) -> {
            Account account = ctx.repo().get(Account.class, t.account);
            ctx.data().put("exists", true);
            account.add(t.amount);
            // then some exception happens eventually
            if (t.amount == 100500)
                throw new RuntimeException("Amount is zero!");
        }, (t, ctx) -> {
            // we shouldn't decrement balance if we are not sure that it actually used to happen
            if (ctx.data().containsKey("exists")) {
                ctx.repo().get(Account.class, t.account).add(-t.amount);
            }
        });

        reveno.domain().viewMapper(Account.class, AccountView.class, (a, b, c) -> new AccountView(b.balance));
        reveno.startup();

        long acc = reveno.executeSync("createAccount", MapUtils.map("balance", 1000l));
        // this is normal execution which won't lead to any issue
        reveno.executeCommand(new AddToBalance(acc, 5)).get();
        long goodIncrementBalance = reveno.query().find(AccountView.class, acc).balance;
        LOG.info("Balance after fine command: {}", goodIncrementBalance);

        // try wrong account ID, in which case exception will be thrown by Tx Action,
        // but there should be no compensating action decrement of balance
        reveno.executeCommand(new AddToBalance(acc + 1, 5)).get();
        long wrongCommandBalance = reveno.query().find(AccountView.class, acc).balance;
        LOG.info("Balance after wrong command: {}", wrongCommandBalance);

        // this will lead to compensating action to be executed, decrementing balance back to its value
        // so final balance will be 1005, not 101505
        reveno.executeCommand(new AddToBalance(acc, 100500)).get();
        wrongCommandBalance = reveno.query().find(AccountView.class, acc).balance;
        LOG.info("Balance after wrong command: {}", wrongCommandBalance);

        reveno.shutdown();
    }

    public static class Account {
        public final long id;
        public long balance;

        public Account(long id, long balance) {
            this.id = id;
            this.balance = balance;
        }

        public void add(long amount) {
            this.balance += amount;
        }
    }

    public static class AccountView {
        public final long balance;

        public AccountView(long balance) {
            this.balance = balance;
        }
    }

    public static class AddToBalance {
        public final long account;
        public final long amount;

        public AddToBalance(long account, long amount) {
            this.account = account;
            this.amount = amount;
        }
    }

}
