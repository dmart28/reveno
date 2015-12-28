package org.reveno.atp.acceptance.tests;

import org.junit.Assert;
import org.junit.Test;
import org.reveno.atp.api.Reveno;
import org.reveno.atp.utils.MapUtils;

public class DslBasedTests extends RevenoBaseTest {

    @Test
    public void testRepositoryMerge() throws Exception {
        Reveno reveno = createEngine();
        reveno.config().mutableModel();
        reveno.domain().transaction("createAccount", (c, d) ->
                d.repo().store(c.id(), new Account()))
                .uniqueIdFor(Account.class).command();
        reveno.domain().transaction("addBalance", (c, d) ->
                d.repo().remap(c.longArg(), Account.class, (id, e) -> e.add(c.longArg("amount"))))
                .command();
        reveno.domain().transaction("createAndAdd", (c, d) ->
                d.repo().merge(c.longArg(), Account.class, () -> new Account().add(c.longArg("amount")),
                        (id, e) -> e.add(c.longArg("amount"))))
                .command();
        reveno.domain().viewMapper(Account.class, Account.class, (a,b,c) -> b);

        reveno.startup();

        long accountId = reveno.executeSync("createAccount");
        reveno.executeSync("addBalance", MapUtils.map("id", accountId, "amount", 10L));
        Assert.assertEquals(reveno.query().find(Account.class, accountId).balance, 10L);

        reveno.executeSync("createAndAdd", MapUtils.map("id", 11L, "amount", 15L));
        Assert.assertEquals(reveno.query().find(Account.class, 11L).balance, 15L);

        reveno.executeSync("createAndAdd", MapUtils.map("id", 11L, "amount", 21L));
        Assert.assertEquals(reveno.query().find(Account.class, 11L).balance, 36L);

        reveno.shutdown();
    }

    public static class Account {
        public long balance = 0;

        public Account add(long amount) {
            balance += amount;
            return this;
        }
    }

}
