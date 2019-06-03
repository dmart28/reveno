package org.reveno.atp.examples.events;

import org.reveno.atp.api.Reveno;
import org.reveno.atp.core.Engine;
import org.reveno.atp.utils.MapUtils;

import java.util.concurrent.atomic.AtomicLong;

import static org.reveno.atp.examples.events.BasicEvents.*;

/**
 * This example shows simple example of dealing with events
 * in Reveno. Events are always published from Transaction Actions, and executed by handlers,
 * which in it's turn can be either sync or async.
 * <p>
 * In this example we will have async Event Handler, which is executed by thread
 * pool, thus with no ordering guarantees. After events executes successfully, Reveno
 * journals that fact, so on replay they will not be fired again.
 */
public class AsyncEvents {

    protected static final AtomicLong counter = new AtomicLong();

    public static void main(String[] args) {
        Reveno reveno = new Engine(args[0]);
        init(reveno);
        reveno.startup();
        long accId = reveno.executeSync("createAccount", MapUtils.map("balance", (long) (3.14 * PRECISION)));
        showBalance(reveno, accId);
        for (int i = 0; i < 1000; i++)
            reveno.execute("changeBalance", MapUtils.map("acc", accId, "amount", (long) (Math.random() * 10 * PRECISION)));
        reveno.executeSync("changeBalance", MapUtils.map("acc", accId, "amount", 0L));
        showBalance(reveno, accId);
        // after even sync command call event handler is not guaranteed to be called, it will do eventually
        reveno.shutdown();
    }

    protected static void showBalance(Reveno reveno, long accId) {
        LOG.info("Account balance: {}", reveno.query().find(AccountView.class, accId).balance);
    }

    protected static void init(Reveno reveno) {
        reveno.domain().transaction("createAccount", (t, c) -> {
            c.repo().store(t.id(), new BasicEvents.Account(t.id(), t.longArg("balance")));
        }).uniqueIdFor(Account.class).conditionalCommand((cmd, c) -> cmd.longArg("balance") >= 0l).command();

        reveno.domain().transaction("changeBalance", (t, c) -> {
            Account account = c.repo().get(Account.class, t.longArg());
            c.repo().store(t.longArg(), account.add(t.longArg("amount")));
            c.eventBus().publishEvent(new BalanceAddedEvent(t.longArg(), t.longArg("amount")));
        }).conditionalCommand((cmd, c) -> c.repo().has(Account.class, cmd.longArg())).command();

        reveno.domain().viewMapper(Account.class, AccountView.class, (e, c, r) -> new AccountView(c.balance / PRECISION));

        // set async handler thread pool size to 10 threads
        reveno.events().asyncEventExecutors(10);
        reveno.events().asyncEventHandler(BalanceAddedEvent.class, (e, m) -> {
            LOG.info("Thread: {}, Event {}: {}", Thread.currentThread().getName(), counter.incrementAndGet(), e);
            LOG.info("Is replay: {}", m.isRestore());
            try {
                Thread.sleep(100);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        });
    }

}
