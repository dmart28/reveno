/** 
 *  Copyright (c) 2015 The original author or authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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
