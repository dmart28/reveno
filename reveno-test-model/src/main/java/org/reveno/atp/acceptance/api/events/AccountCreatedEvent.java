package org.reveno.atp.acceptance.api.events;

public class AccountCreatedEvent {
	public final long accountId;
	
	public AccountCreatedEvent(long accountId) {
		this.accountId = accountId;
	}
}
