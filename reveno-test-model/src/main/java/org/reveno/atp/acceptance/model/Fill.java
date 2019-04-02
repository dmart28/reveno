package org.reveno.atp.acceptance.model;

public interface Fill extends Changeable {

	long id();
	
	long accountId();
	
	long positionId();
	
	long size();
	
	long price();
	
	Order order();
	
	
	public interface FillFactory {
		Fill create(long id, long accountId, long positionId, long size, long price, Order order);
	}
	
}
