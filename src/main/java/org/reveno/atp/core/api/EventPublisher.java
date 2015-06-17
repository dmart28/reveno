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

import org.reveno.atp.api.EventsManager.EventMetadata;

/**
 * EventPublisher is main entry point in system, at which all events
 * from transaction are being sent to execution.
 * 
 * @author Artem Dmitriev <art.dm.ser@gmail.com>
 *
 */
public interface EventPublisher {
	
	void start();
	
	void stop();
	
	void shutdown();
	
	void sync();

	void publishEvents(boolean isReplay, long transactionId, EventMetadata eventMetadata, Object[] events);

	void commitAsyncError(boolean isReplay, long transactionId);
	
	
	public static final int ASYNC_ERROR_FLAG = 0xBABECAFE;
	public static final int SYNC_FLAG = 0xCAFEBABE;
	
}
