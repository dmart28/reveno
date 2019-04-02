package org.reveno.atp.core.api;

/**
 * Contains basic info about events execution result, which were issued in some transaction.
 * We have to keep track of whether all events in transaction were correctly
 * handled, and if so, commit info about it.
 * 
 * On restore we don't re-issue events, which were already committed.
 * 
 * @author Artem Dmitriev <art.dm.ser@gmail.com>
 *
 */
public interface EventsCommitInfo {
	
	/**
	 * Transaction ID, in which events were executed.
	 * 
	 * @return transactionId
	 */
	long transactionId();
	
	/**
	 * The time of events execution.
	 * 
	 * @return time
	 */
	long time();
	
	/**
	 * Some flag of events commit for internal handling or restore process.
	 * 
	 * @return flag
	 */
	long flag();
	
	
	interface Builder {
		EventsCommitInfo create(long txId, long time, long flag);
	}
	
}
