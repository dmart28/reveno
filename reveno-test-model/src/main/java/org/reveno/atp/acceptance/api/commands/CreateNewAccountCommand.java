package org.reveno.atp.acceptance.api.commands;

import java.io.Serializable;

public class CreateNewAccountCommand implements Serializable {
	public final String currency;
	public final long balance;
	
	public CreateNewAccountCommand(String currency, long balance) {
		this.currency = currency;
		this.balance = balance;
	}
}
