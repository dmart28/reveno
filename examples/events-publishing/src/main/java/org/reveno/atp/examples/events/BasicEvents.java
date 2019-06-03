package org.reveno.atp.examples.events;

import org.reveno.atp.api.Reveno;
import org.reveno.atp.core.Engine;
import org.reveno.atp.utils.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This example shows simple example of dealing with events
 * in Reveno. Events are always published from Transaction Actions, and executed by handlers,
 * which in it's turn can be either sync or async.
 * <p>
 * In this example we will have sync Event Handler, which guarantees ordering
 * and executed by single thread only. After events executes successfully, Reveno
 * journals that fact, so on replay they will not be fired again.
 */
public class BasicEvents {
    public static final Logger LOG = LoggerFactory.getLogger(BasicEvents.class);
    public static final double PRECISION = 10000;

    public static void main(String[] args) {
        Reveno reveno = new Engine(args[0]);
        init(reveno);
        reveno.startup();
        long accId = reveno.executeSync("createAccount", MapUtils.map("balance", (long) (3.14 * PRECISION)));
        showBalance(reveno, accId);
        reveno.executeSync("changeBalance", MapUtils.map("acc", accId, "amount", (long) (2.86 * PRECISION)));
        showBalance(reveno, accId);
        // after even sync command call event handler is not guaranteed to be called, it will do eventually
        reveno.shutdown();
    }

    protected static void showBalance(Reveno reveno, long accId) {
        LOG.info("Account balance: {}", reveno.query().find(AccountView.class, accId).balance);
    }

    protected static void init(Reveno reveno) {
        reveno.domain().transaction("createAccount", (t, c) -> {
            c.repo().store(t.id(), new Account(t.id(), t.longArg("balance")));
        }).uniqueIdFor(Account.class).conditionalCommand((cmd, c) -> cmd.longArg("balance") >= 0L).command();

        reveno.domain().transaction("changeBalance", (t, c) -> {
            c.repo().remap(t.longArg(), Account.class, (id, a) -> a.add(t.longArg("amount")));
            c.eventBus().publishEvent(new BalanceAddedEvent(t.longArg(), t.longArg("amount")));
        }).conditionalCommand((cmd, c) -> c.repo().has(Account.class, cmd.longArg())).command();

        reveno.domain().viewMapper(Account.class, AccountView.class, (e, c, r) -> new AccountView(c.balance / PRECISION));
        reveno.events().eventHandler(BalanceAddedEvent.class, (e, m) -> {
            LOG.info("Event: {}", e);
            LOG.info("Is on replay: {}", m.isRestore());
        });
    }

    public static class Account {
        public final long id;
        public final long balance;

        public Account(long id, long balance) {
            this.id = id;
            this.balance = balance;
        }

        public Account add(long amount) {
            return new Account(id, balance + amount);
        }
    }

    public static class AccountView {
        public final double balance;

        public AccountView(double balance) {
            this.balance = balance;
        }
    }

    public static class BalanceAddedEvent {
        public final long accountId;
        public final long amount;

        public BalanceAddedEvent(long accountId, long amount) {
            this.accountId = accountId;
            this.amount = amount;
        }

        @Override
        public String toString() {
            return "BalanceAddedEvent{" +
                    "accountId=" + accountId +
                    ", amount=" + amount +
                    '}';
        }
    }

}
