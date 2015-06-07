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

package org.reveno.atp.core;

import org.reveno.atp.core.api.Journaler;
import org.reveno.atp.core.api.storage.JournalsStorage;
import org.reveno.atp.core.api.storage.JournalsStorage.JournalStore;

public class JournalsRoller {

	public synchronized void roll(Runnable completed) {
		JournalStore store = storage.nextStore();
		eventsJournaler.roll(storage.channel(store.getEventsCommitsAddress()), ()->{});
		transactionsJournaler.roll(storage.channel(store.getTransactionCommitsAddress()), completed);
	}
	
	public JournalsRoller(Journaler transactionsJournaler, Journaler eventsJournaler,
			JournalsStorage storage) {
		this.transactionsJournaler = transactionsJournaler;
		this.eventsJournaler = eventsJournaler;
		this.storage = storage;
	}
	
	protected Journaler transactionsJournaler;
	protected Journaler eventsJournaler;
	protected JournalsStorage storage;
	
}
