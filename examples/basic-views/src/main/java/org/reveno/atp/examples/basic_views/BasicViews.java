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

package org.reveno.atp.examples.basic_views;

import org.reveno.atp.api.Reveno;
import org.reveno.atp.core.Engine;
import org.reveno.atp.test.utils.LongUtils;
import org.reveno.atp.utils.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BasicViews {

    protected static final Logger LOG = LoggerFactory.getLogger(BasicViews.class);

    public static final long PRECISION = 10000;
    public static final BigDecimal PRECISION_BD = new BigDecimal(PRECISION);

    public static void main(String[] args) throws Exception {
        Reveno reveno = new Engine(args[0]);
        reveno.config().mutableModel();

        reveno.domain().transaction("createTrader", (tx, ctx) ->
            ctx.repo().store(tx.id(), new Trader(tx.id()))
        ).uniqueIdFor(Trader.class).command();
        reveno.domain().transaction("createOrder", (tx, ctx) -> {
            ctx.repo().remap(Trader.class, tx.arg("trId"), (id, t) -> t.orders.add(tx.id()));
            ctx.repo().store(tx.id(), new Order(tx.id(), tx.longArg("size"), tx.longArg("price")));
        }).uniqueIdFor(Order.class).command();

        reveno.domain().viewMapper(Trader.class, TraderView.class, (id, e, r) ->
                new TraderView(e.id, r.link(e.orders.stream(), OrderView.class)));
        reveno.domain().viewMapper(Order.class, OrderView.class, (id, e, r) ->
                new OrderView(e.id, e.size, e.price));

        reveno.startup();

        long traderId = reveno.executeSync("createTrader");
        long orderId = reveno.executeSync("createOrder", MapUtils.map("trId", traderId, "size", 1000L, "price", (long)(3.14 * PRECISION)));

        TraderView trader = reveno.query().find(TraderView.class, traderId);
        LOG.info("Trader: {}", trader);
        OrderView order = reveno.query().find(OrderView.class, orderId);
        LOG.info("Order: {}", order);

        reveno.shutdown();
    }

    public static class Trader {
        public final long id;
        public final List<Long> orders = new ArrayList<>();

        public Trader(long id) {
            this.id = id;
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
