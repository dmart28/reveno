package org.reveno.atp.examples.snapshotting;

import org.reveno.atp.api.Reveno;
import org.reveno.atp.core.Engine;
import org.reveno.atp.test.utils.FileUtils;
import org.reveno.atp.test.utils.LongUtils;
import org.reveno.atp.utils.MapUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public class SnapshottingExample {

    public static final long PRECISION = 10000;
    public static final BigDecimal PRECISION_BD = new BigDecimal(PRECISION);

    public static void main(String[] args) throws Exception {
        //System.setProperty("protostuff.runtime.collection_schema_on_repeated_fields", "true");

        Reveno reveno = initReveno(args);
        reveno.startup();

        long traderId = reveno.executeSync("createTrader");
        for (long i = 0; i < 10_000; i++) {
            reveno.execute("createOrder", MapUtils.map("trId", traderId, "size", i, "price", 1300L * i));
        }
        reveno.executeSync("createTrader");

        reveno.shutdown();

        /// We check to see that snapshot was actually done
        System.out.println("Snapshot file: " + FileUtils.findFile(args[0], "snp-"));

        reveno = initReveno(args);
        reveno.startup();

        System.out.println("Traders: " + reveno.query().select(TraderView.class).size());
        System.out.println("Orders: " + reveno.query().select(OrderView.class).size());

        reveno.shutdown();

        FileUtils.clearFolder(new java.io.File(args[0]));
    }

    protected static Reveno initReveno(String[] args) {
        Reveno reveno = new Engine(args[0], SnapshottingExample.class.getClassLoader());
        /// This example goes for both mutable and immutable model types
        String model = args.length == 1 ? "mutable" : args[1];
        if (model.equals("mutable")) {
            reveno.config().mutableModel();
        }
        /// We are making snapshots on every 10,001 transaction
        reveno.config().snapshotting().every(10_000);

        reveno.domain().transaction("createTrader", (tx, ctx) ->
                ctx.repo().store(tx.id(), new Trader(tx.id()))
        ).uniqueIdFor(Trader.class).command();

        reveno.domain().transaction("createOrder", (tx, ctx) -> {
            if (model.equals("mutable")) {
                ctx.repo().remap(Trader.class, tx.longArg("trId"), (id, t) -> t.addMutable(tx.id()));
            } else {
                ctx.repo().remap(tx.longArg("trId"), Trader.class, (id, t) -> t.addImmutable(tx.id()));
            }
            ctx.repo().store(tx.id(), new Order(tx.id(), tx.longArg("size"), tx.longArg("price")));
        }).uniqueIdFor(Order.class).command();

        reveno.domain().viewMapper(Trader.class, TraderView.class, (id, e, r) ->
                new TraderView(e.id, r.link(e.orders, OrderView.class)));
        reveno.domain().viewMapper(Order.class, OrderView.class, (id, e, r) ->
                new OrderView(e.id, e.size, e.price));

        return reveno;
    }

    /**
     * Since this model classes don't implement Serializable interface, they will be
     * snapshotted using Protostuff serializer, not Java default.
     * <p>
     * You can make it Serializable and check the result by yourself.
     */
    public static class Trader {
        public final long id;
        public long[] orders;

        public Trader(long id) {
            this(id, new long[0]);
        }

        public Trader(long id, long[] orders) {
            this.id = id;
            this.orders = orders;
        }

        public void addMutable(long order) {
            long[] newOrders = new long[orders.length + 1];
            System.arraycopy(orders, 0, newOrders, 0, orders.length);
            newOrders[orders.length] = order;
            this.orders = newOrders;
        }

        public Trader addImmutable(long order) {
            long[] newOrders = new long[orders.length + 1];
            System.arraycopy(orders, 0, newOrders, 0, orders.length);
            newOrders[orders.length] = order;
            return new Trader(id, newOrders);
        }
    }

    public static class Order {
        public final long id;
        public final long size;
        public final long price;

        public Order(long id, long size, long price) {
            this.id = id;
            this.size = size;
            this.price = price;
        }
    }

    public static class TraderView {
        public final String id;
        public final List<OrderView> orders;

        public TraderView(long id, List<OrderView> orders) {
            this.id = LongUtils.longToBase64(id);
            this.orders = Collections.unmodifiableList(orders);
        }

        @Override
        public String toString() {
            return "TraderView{" +
                    "id='" + id + '\'' +
                    ", orders=" + orders +
                    '}';
        }
    }

    public static class OrderView {
        public final String id;
        public final BigDecimal price;
        public final long amount;

        public OrderView(long id, long size, long price) {
            this.id = LongUtils.longToBase64(id);
            this.price = new BigDecimal(price).divide(PRECISION_BD);
            this.amount = size;
        }

        @Override
        public String toString() {
            return "OrderView{" +
                    "id='" + id + '\'' +
                    ", price=" + price +
                    ", amount=" + amount +
                    '}';
        }
    }

}
