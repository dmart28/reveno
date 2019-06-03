package org.reveno.atp.acceptance.api.transactions;

import java.io.Serializable;

public class CreateAccount implements Serializable {
    public final long id;
    public final String currency;

    public CreateAccount(long id, String currency) {
        this.id = id;
        this.currency = currency;
    }
}
