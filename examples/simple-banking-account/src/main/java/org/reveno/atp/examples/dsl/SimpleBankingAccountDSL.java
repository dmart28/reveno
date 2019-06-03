package org.reveno.atp.examples.dsl;

import org.reveno.atp.api.Reveno;
import org.reveno.atp.core.Engine;
import org.reveno.atp.examples.Account;
import org.reveno.atp.examples.AccountView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.reveno.atp.utils.MapUtils.map;

public class SimpleBankingAccountDSL {

    protected static final Logger LOG = LoggerFactory.getLogger(SimpleBankingAccountDSL.class);

    public static Reveno init(String folder) {
        Reveno reveno = new Engine(folder);
        reveno.domain()
                .transaction("createAccount", (t, c) -> c.repo().store(t.id(), new Account(t.arg(), 0)))
                .uniqueIdFor(Account.class)
                .command();
        reveno.domain()
                .transaction("changeBalance", (t, c) ->
                        c.repo().store(t.longArg(), c.repo().get(Account.class, t.arg()).add(t.intArg("inc"))))
                .command();
        reveno.domain().viewMapper(Account.class, AccountView.class, (id, e, r) -> new AccountView(id, e.name, e.balance));
        return reveno;
    }

    protected static void printStats(Reveno reveno, long accountId) {
        LOG.info("Account {} name: {}", accountId, reveno.query().find(AccountView.class, accountId).name);
        LOG.info("Account {} balance: {}", accountId, reveno.query().find(AccountView.class, accountId).balance);
    }

    public static void main(String[] args) {
        Reveno reveno = init(args[0]);
        reveno.startup();

        long accountId = reveno.executeSync("createAccount", map("name", "John"));
        reveno.executeSync("changeBalance", map("id", accountId, "inc", 10_000));

        printStats(reveno, accountId);
        reveno.shutdown();

        reveno = init(args[0]);
        reveno.startup();

        // we perform no operations here, just looking at last restored state
        printStats(reveno, accountId);

        reveno.shutdown();
    }

}
