package org.reveno.atp.acceptance.model;

import java.util.Collection;
import java.util.List;

public interface Position extends Changeable {

	long id();
	
	String symbol();
	
	long accountId();
	
	List<Fill> fills();
	
	Position addFill(Fill fill);
	
	Position addFills(Collection<Fill> fills);
	
	boolean buy();
	
	boolean isComplete();
	
	long sum();
	
	
	public interface PositionFactory {
		Position create(long id, String symbol, long accountId, Fill fill);
	}
	
}
