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

package org.reveno.atp.core.engine.components;

import org.reveno.atp.api.Configuration;
import org.reveno.atp.api.RepositorySnapshotter;
import org.reveno.atp.api.RepositorySnapshotter.SnapshotIdentifier;
import org.reveno.atp.api.domain.RepositoryData;
import org.reveno.atp.api.domain.WriteableRepository;
import org.reveno.atp.api.transaction.TransactionInterceptor;
import org.reveno.atp.api.transaction.TransactionStage;
import org.reveno.atp.core.JournalsManager;
import org.reveno.atp.core.RevenoConfiguration;
import org.reveno.atp.core.api.SystemInfo;
import org.reveno.atp.core.api.storage.JournalsStorage;
import org.reveno.atp.core.api.storage.SnapshotStorage;
import org.reveno.atp.core.snapshots.SnapshottersManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

public class SnapshottingInterceptor implements TransactionInterceptor {
	
	protected long counter = 1L;
	protected Map<Long, SnapshotIdentifier[]> snapshots = new ConcurrentHashMap<>();
	protected Map<Long, Future<?>> futures = new ConcurrentHashMap<>();

	@Override
	public void intercept(long transactionId, long time, WriteableRepository repository, TransactionStage stage) {
		if (stage == TransactionStage.TRANSACTION) {
			if (counter++ % configuration.revenoSnapshotting().every() == 0) {
				asyncSnapshot(repository.getData(), transactionId);
				if (configuration.modelType() == Configuration.ModelType.MUTABLE) {
					try {
						futures.remove(transactionId).get();
					} catch (InterruptedException | ExecutionException ignored) {
					}
				}
			}
		} else if (stage == TransactionStage.JOURNALING && snapshots.containsKey(transactionId)) {
			if (configuration.modelType() != Configuration.ModelType.MUTABLE) {
				try {
					futures.remove(transactionId).get();
				} catch (InterruptedException | ExecutionException e) {
					return;
				}
			}
			try {
				SnapshotIdentifier[] ids = snapshots.remove(transactionId);
				final List<RepositorySnapshotter> snaps = snapshotsManager.getAll();
				for (int i = 0; i < ids.length; i++) {
					snaps.get(i).commit(ids[i]);
				}
			} finally {
				journalsManager.roll(transactionId);
			}
		}
	}
	
	@Override
	public void destroy() {
		if (!executor.isShutdown()) {
			executor.shutdown();
			try {
				executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			} catch (InterruptedException e) {
				LOG.error(e.getMessage(), e);
			}
		}
	}

	private void asyncSnapshot(RepositoryData data, long transactionId) {
		data.data.computeIfAbsent(SystemInfo.class, k -> new HashMap<>()).put(0L, new SystemInfo(transactionId));
		final List<RepositorySnapshotter> snaps = snapshotsManager.getAll();
		final SnapshotIdentifier[] ids = new SnapshotIdentifier[snaps.size()];
		final long lastJournalVersion = journalsStorage.getLastStoreVersion();
		for (int i = 0; i < snaps.size(); i++) {
			ids[i] = snaps.get(i).prepare(lastJournalVersion);
		}
		// hack to not box long two times
		Long boxed = transactionId;
		futures.put(boxed, executor.submit(() -> {
			for (int i = 0; i < snaps.size(); i++) {
				try {
					snaps.get(i).snapshot(data, ids[i]);
				} catch (Throwable t) {
					LOG.error(t.getMessage(), t);
				}
			}
		}));
		snapshots.put(boxed, ids);
	}
	
	public SnapshottingInterceptor(RevenoConfiguration configuration,
								   SnapshottersManager snapshotsManager, SnapshotStorage snapshotStorage,
								   JournalsStorage journalsStorage, JournalsManager journalsManager) {
		this.configuration = configuration;
		this.snapshotsManager = snapshotsManager;
		this.journalsManager = journalsManager;
		this.journalsStorage = journalsStorage;
		this.snapshotStorage = snapshotStorage;
	}
	
	protected RevenoConfiguration configuration;
	protected SnapshottersManager snapshotsManager;
	protected JournalsStorage journalsStorage;
	protected SnapshotStorage snapshotStorage;
	protected JournalsManager journalsManager;
	protected final ExecutorService executor = Executors.newSingleThreadExecutor();
	protected static final Logger LOG = LoggerFactory.getLogger(SnapshottingInterceptor.class);

}
