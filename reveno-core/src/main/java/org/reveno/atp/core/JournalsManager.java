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
import org.reveno.atp.core.data.DefaultJournaler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class JournalsManager implements Destroyable {

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
		log.debug("Trying to roll to next store.");
		isRolling = true;

		try {
			if (!condition.get()) {
				return;
			}
			log.info("Rolling to next store.");
			JournalStore store;

			if (configuration.isPreallocated() && configuration.volumes() > 0 && storage.getVolumes().length == 0) {
				IntStream.range(0, configuration.volumes()).forEach(i -> allocateNewVolume(true));
				roll(completed);
				return;
			} else if (configuration.isPreallocated() && configuration.volumes() > 0) {
				allocateNewVolumeIfRequired();
				store = storage.convertVolumeToStore(storage.getVolumes()[0]);
			} else {
				store = storage.nextStore();
			}
			eventsJournaler.roll(storage.channel(store.getEventsCommitsAddress()), () -> {});
			transactionsJournaler.roll(storage.channel(store.getTransactionCommitsAddress()), completed);
		} finally {
			isRolling = false;
		}
	}

	public boolean isRolling() {
		return isRolling;
	}

	public Journaler getTransactionsJournaler() {
		return transactionsJournaler;
	}

	public Journaler getEventsJournaler() {
		return eventsJournaler;
	}

	protected void allocateNewVolumeIfRequired() {
		if (storage.getVolumes().length <= configuration.minVolumes()) {
			log.info("Allocating {} new volumes", configuration.minVolumes());
			IntStream.range(0, configuration.minVolumes() + 1).forEach(i -> allocateNewVolume(false));
        }
	}

	protected void allocateNewVolume(boolean alwaysWait) {
		Future<?> f = prepareNextVolume();
		if (storage.getVolumes().length <= 1 || alwaysWait) {
            try {
                f.get();
            } catch (InterruptedException | ExecutionException ignored) {
            }
        }
	}

	protected Future<?> prepareNextVolume() {
		return executor.submit(() -> { try {
			storage.nextVolume(configuration.txSize(), configuration.eventsSize());
		} catch (Throwable t) {
			log.error("prepareNextVolume", t);
		}});
	}

	@Override
	public void destroy() {
		executor.shutdown();
		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		} catch (InterruptedException ignored) {
		}
		transactionsJournaler.destroy();
		eventsJournaler.destroy();
	}
	
	public JournalsManager(JournalsStorage storage, RevenoJournalingConfiguration configuration) {
		this.storage = storage;
		this.configuration = configuration;
		this.transactionsJournaler = new DefaultJournaler();
		this.eventsJournaler = new DefaultJournaler();
	}

	protected volatile boolean isRolling = false;
	protected Journaler transactionsJournaler;
	protected Journaler eventsJournaler;
	protected JournalsStorage storage;
	protected RevenoJournalingConfiguration configuration;
	protected ExecutorService executor = Executors.newSingleThreadExecutor();

	protected static final Logger log = LoggerFactory.getLogger(JournalsManager.class);

}
