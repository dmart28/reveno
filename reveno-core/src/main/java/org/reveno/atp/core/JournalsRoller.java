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

import org.reveno.atp.core.RevenoConfiguration.RevenoJournalingConfiguration;
import org.reveno.atp.core.api.Destroyable;
import org.reveno.atp.core.api.Journaler;
import org.reveno.atp.core.api.storage.JournalsStorage;
import org.reveno.atp.core.api.storage.JournalsStorage.JournalStore;

import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class JournalsRoller implements Destroyable {

	public void roll() {
		roll(() -> {}, () -> true);
	}

	public void roll(Runnable completed) {
		roll(completed, () -> true);
	}

	public void roll(Supplier<Boolean> condition) {
		roll(() -> {}, condition);
	}

	public synchronized void roll(Runnable completed, Supplier<Boolean> condition) {
		isRolling = true;

		try {
			if (!condition.get()) {
				return;
			}
			JournalStore store;

			if (configuration.isPreallocated() && configuration.volumes() > 0) {
				allocateNewVolumeIfRequired();
				store = storage.convertVolumeToStore(storage.getVolumes()[0]);
			} else if (configuration.isPreallocated() && configuration.volumes() > 0
					&& storage.getVolumes().length == 0) {
				IntStream.range(0, configuration.volumes()).forEach(i -> allocateNewVolumeIfRequired());
				roll(completed);
				return;
			} else {
				store = storage.nextStore();
			}
			eventsJournaler.roll(storage.channel(store.getEventsCommitsAddress()), () -> {
			});
			transactionsJournaler.roll(storage.channel(store.getTransactionCommitsAddress()), completed);
		} finally {
			isRolling = false;
		}
	}

	public boolean isRolling() {
		return isRolling;
	}

	protected void allocateNewVolumeIfRequired() {
		if (storage.getVolumes().length <= configuration.minVolumes()) {
            Future<?> f = prepareNextVolume();
            if (storage.getVolumes().length == 1) {
                try {
                    f.get();
                } catch (InterruptedException | ExecutionException ignored) {
                }
            }
        }
	}

	protected Future<?> prepareNextVolume() {
		return executor.submit(() -> storage.nextVolume(configuration.txSize(), configuration.eventsSize()));
	}

	@Override
	public void destroy() {
		executor.shutdown();
		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		} catch (InterruptedException ignored) {
		}
	}
	
	public JournalsRoller(Journaler transactionsJournaler, Journaler eventsJournaler,
			JournalsStorage storage, RevenoJournalingConfiguration configuration) {
		this.transactionsJournaler = transactionsJournaler;
		this.eventsJournaler = eventsJournaler;
		this.storage = storage;
		this.configuration = configuration;
	}

	protected volatile boolean isRolling = false;
	protected Journaler transactionsJournaler;
	protected Journaler eventsJournaler;
	protected JournalsStorage storage;
	protected RevenoJournalingConfiguration configuration;
	protected ExecutorService executor = Executors.newSingleThreadExecutor();

}
