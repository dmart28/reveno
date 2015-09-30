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

import org.reveno.atp.api.Configuration.ModelType;
import org.reveno.atp.api.RepositorySnapshotter;
import org.reveno.atp.api.RepositorySnapshotter.SnapshotIdentifier;
import org.reveno.atp.api.domain.RepositoryData;
import org.reveno.atp.api.domain.WriteableRepository;
import org.reveno.atp.api.transaction.TransactionInterceptor;
import org.reveno.atp.api.transaction.TransactionStage;
import org.reveno.atp.core.JournalsManager;
import org.reveno.atp.core.RevenoConfiguration;
import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.api.serialization.RepositoryDataSerializer;
import org.reveno.atp.core.api.storage.SnapshotStorage;
import org.reveno.atp.core.api.storage.SnapshotStorage.SnapshotStore;
import org.reveno.atp.core.channel.NettyBasedBuffer;
import org.reveno.atp.core.snapshots.SnapshottersManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SnapshottingInterceptor implements TransactionInterceptor {
	
	protected long counter = 1L;
	protected Map<Long, Buffer> snapshotsMutable = new ConcurrentHashMap<>();
	protected Map<Long, RepositoryData> snapshotsImmutable = new ConcurrentHashMap<>();

	@Override
	public void intercept(long transactionId, long time, WriteableRepository repository, TransactionStage stage) {
			if (stage == TransactionStage.TRANSACTION) {
				if (counter++ % configuration.revenoSnapshotting().every() == 0) {
					if (configuration.modelType() == ModelType.MUTABLE) {
						NettyBasedBuffer buffer = new NettyBasedBuffer();
						serializer.serialize(repository.getData(), buffer);
						snapshotsMutable.put(transactionId, buffer);
					} else {
						snapshotsImmutable.put(transactionId, repository.getData());
					}
				}
			} else if (stage == TransactionStage.JOURNALING) {
				if (snapshotsMutable.containsKey(transactionId) || snapshotsImmutable.containsKey(transactionId)) {
					if (configuration.modelType() == ModelType.MUTABLE) {
						Buffer buffer = snapshotsMutable.get(transactionId);
						RepositoryData data = serializer.deserialize(buffer);
						buffer.release();
						snapshotsMutable.remove(transactionId);

						asyncSnapshot(data);
					} else {
						asyncSnapshot(snapshotsImmutable.get(transactionId));
						snapshotsImmutable.remove(transactionId);
					}
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
				log.error(e.getMessage(), e);
			}
		}
	}

	private void asyncSnapshot(RepositoryData data) {
		final Collection<RepositorySnapshotter> snaps = snapshotsManager.getAll();
		// TODO optimise!
		final List<SnapshotIdentifier> ids = snaps.stream()
				.map(RepositorySnapshotter::prepare)
				.collect(Collectors.toList());
		executor.submit(() -> {
			int count = 0;
			for (Iterator<RepositorySnapshotter> i = snaps.iterator(); i.hasNext();) {
				i.next().snapshot(data, ids.get(count++));
			}
		});
	}
	
	public SnapshottingInterceptor(RevenoConfiguration configuration,
			SnapshottersManager snapshotsManager, SnapshotStorage snapshotStorage,
			JournalsManager journalsManager, RepositoryDataSerializer serializer) {
		this.configuration = configuration;
		this.snapshotsManager = snapshotsManager;
		this.journalsManager = journalsManager;
		this.serializer = serializer;
		this.snapshotStorage = snapshotStorage;
	}
	
	protected SnapshotStore nextStore() {
		return snapshotStorage.nextSnapshotStore();
	}
	
	protected RevenoConfiguration configuration;
	protected SnapshottersManager snapshotsManager;
	protected SnapshotStorage snapshotStorage;
	protected JournalsManager journalsManager;
	protected RepositoryDataSerializer serializer;
	protected final ExecutorService executor = Executors.newSingleThreadExecutor();
	protected static final Logger log = LoggerFactory.getLogger(SnapshottingInterceptor.class);

}
