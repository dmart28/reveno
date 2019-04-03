package org.reveno.atp.acceptance.api.transactions;

import java.io.Serializable;

public class Credit implements Serializable {
    public final long accountId;
    public final long amount;
    public long time;

    public Credit(long accountId, long amount, long time) {
        this.accountId = accountId;
        this.amount = amount;
        this.time = time;
    }
}
