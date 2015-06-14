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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.reveno.atp.api.Configuration.ModelType;
import org.reveno.atp.api.domain.RepositoryData;
import org.reveno.atp.api.domain.WriteableRepository;
import org.reveno.atp.api.transaction.TransactionInterceptor;
import org.reveno.atp.api.transaction.TransactionStage;
import org.reveno.atp.core.JournalsRoller;
import org.reveno.atp.core.RevenoConfiguration;
import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.api.serialization.RepositoryDataSerializer;
import org.reveno.atp.core.channel.NettyBasedBuffer;
import org.reveno.atp.core.snapshots.SnapshottersManager;

public class SnapshottingInterceptor implements TransactionInterceptor {
	
	protected long counter = 1L;
	protected Map<Long, Buffer> snapshotsMutable = new ConcurrentHashMap<>();
	protected Map<Long, RepositoryData> snapshotsImmutable = new ConcurrentHashMap<>();

	@Override
	public void intercept(long transactionId, WriteableRepository repository, TransactionStage stage) {
			if (stage == TransactionStage.TRANSACTION) {
				if (counter++ % configuration.revenoSnapshotting().snapshotEvery() == 0) {
					if (configuration.modelType() == ModelType.MUTABLE) {
						NettyBasedBuffer buffer = new NettyBasedBuffer();
						serializer.serialize(repository.getData(), buffer);
						snapshotsMutable.put(transactionId, buffer);
					} else {
						snapshotsImmutable.put(transactionId, repository.getData());
					}
				}
			} else if (stage == TransactionStage.JOURNALING) {
				if (configuration.modelType() == ModelType.MUTABLE && snapshotsMutable.containsKey(transactionId)) {
					Buffer buffer = snapshotsMutable.get(transactionId);
					RepositoryData data = serializer.deserialize(buffer);
					buffer.release();
					snapshotsMutable.remove(transactionId);
					
					snapshotsManager.getAll().forEach(s -> s.snapshot(data));
					roller.roll(() -> {});
				} else if (snapshotsImmutable.containsKey(transactionId)) {
					snapshotsManager.getAll().forEach(s -> s.snapshot(snapshotsImmutable.get(transactionId)));
					snapshotsImmutable.remove(transactionId);
					roller.roll(() -> {});
				}
			}
	}
	
	public SnapshottingInterceptor(RevenoConfiguration configuration,
			SnapshottersManager snapshotsManager, JournalsRoller roller,
			RepositoryDataSerializer serializer) {
		this.configuration = configuration;
		this.snapshotsManager = snapshotsManager;
		this.roller = roller;
		this.serializer = serializer;
	}
	
	protected RevenoConfiguration configuration;
	protected SnapshottersManager snapshotsManager;
	protected JournalsRoller roller;
	protected RepositoryDataSerializer serializer;
	protected static final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

}
