package org.reveno.atp.acceptance.api.transactions;

import java.io.Serializable;

public class Debit implements Serializable {
	public final long accountId;
	public final long amount;
	
	public Debit(long accountId, long amount) {
		this.accountId = accountId;
		this.amount = amount;
	}
}
