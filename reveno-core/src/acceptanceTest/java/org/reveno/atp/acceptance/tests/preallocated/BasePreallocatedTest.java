package org.reveno.atp.acceptance.tests.preallocated;

import org.junit.Assert;
import org.reveno.atp.acceptance.api.commands.CreateNewAccountCommand;
import org.reveno.atp.acceptance.api.commands.NewOrderCommand;
import org.reveno.atp.acceptance.api.events.AccountCreatedEvent;
import org.reveno.atp.acceptance.api.events.OrderCreatedEvent;
import org.reveno.atp.acceptance.model.Order;
import org.reveno.atp.acceptance.tests.RevenoBaseTest;
import org.reveno.atp.acceptance.tests.TestRevenoEngine;
import org.reveno.atp.acceptance.views.AccountView;
import org.reveno.atp.acceptance.views.OrderView;
import org.reveno.atp.api.ChannelOptions;
import org.reveno.atp.core.serialization.DefaultJavaSerializer;

import java.util.Collections;
import java.util.function.Consumer;

public abstract class BasePreallocatedTest extends RevenoBaseTest {
    public static final int LARGE_FILE = 2_500_000;
    public static final int SMALL_FILE = 500_000;
    public static final int EXTRA_SMALL_FILE = 50_000;
    public static final Consumer<TestRevenoEngine> LARGE_CHECKS = reveno -> {
        Assert.assertEquals(9, reveno.getJournalsStorage().getVolumes().length);
        Assert.assertEquals(1, reveno.getJournalsStorage().getAllStores().length);
    };
    public static final Consumer<TestRevenoEngine> SMALL_CHECKS = reveno -> {
        Assert.assertEquals(8, reveno.getJournalsStorage().getVolumes().length);
        Assert.assertEquals(2, reveno.getJournalsStorage().getAllStores().length);
    };

    public void testPreallocatedJournals(long txSize, ChannelOptions channelOptions, Consumer<TestRevenoEngine> checks) throws Exception {
        testPreallocatedJournals(txSize, channelOptions, checks, false);
    }

    public void testPreallocatedJournals(long txSize, ChannelOptions channelOptions, Consumer<TestRevenoEngine> checks,
                                         boolean javaSerializer) throws Exception {
        setUp();

        Consumer<TestRevenoEngine> consumer = r -> {
            r.config().journaling().minVolumes(1);
            r.config().journaling().volumes(10);
            r.config().journaling().volumesSize(txSize, 500_000L);
            r.config().journaling().channelOptions(channelOptions);
            if (javaSerializer) {
                r.domain().serializeWith(Collections.singletonList(new DefaultJavaSerializer()));
            }
        };
        TestRevenoEngine reveno = createEngine(consumer);
        Waiter accountsWaiter = listenFor(reveno, AccountCreatedEvent.class, 5_000);
        Waiter ordersWaiter = listenFor(reveno, OrderCreatedEvent.class, 5_000);
        reveno.startup();

        generateAndSendCommands(reveno, 5_000);

        Assert.assertEquals(5_000, reveno.query().select(AccountView.class).size());
        Assert.assertEquals(5_000, reveno.query().select(OrderView.class).size());

        Assert.assertTrue(accountsWaiter.isArrived());
        Assert.assertTrue(ordersWaiter.isArrived());

        checks.accept(reveno);
        reveno.shutdown();

        reveno = createEngine(consumer);
        accountsWaiter = listenFor(reveno, AccountCreatedEvent.class, 1);
        ordersWaiter = listenFor(reveno, OrderCreatedEvent.class, 1);
        reveno.startup();

        Assert.assertEquals(5_000, reveno.query().select(AccountView.class).size());
        Assert.assertEquals(5_000, reveno.query().select(OrderView.class).size());

        Assert.assertFalse(accountsWaiter.isArrived(1));
        Assert.assertFalse(ordersWaiter.isArrived(1));

        long accountId = sendCommandSync(reveno, new CreateNewAccountCommand("USD", 1000_000L));
        Assert.assertEquals(5_001, accountId);
        long orderId = sendCommandSync(reveno, new NewOrderCommand(accountId, null, "EUR/USD", 134000, 1000, Order.OrderType.MARKET));
        Assert.assertEquals(5_001, orderId);

        reveno.shutdown();

        tearDown();
    }

}
