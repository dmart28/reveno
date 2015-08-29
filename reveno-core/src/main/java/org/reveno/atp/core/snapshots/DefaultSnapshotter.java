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

package org.reveno.atp.core.snapshots;

import org.reveno.atp.api.RepositorySnapshotter;
import org.reveno.atp.api.domain.RepositoryData;
import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.api.channel.Channel;
import org.reveno.atp.core.api.serialization.RepositoryDataSerializer;
import org.reveno.atp.core.api.storage.SnapshotStorage;
import org.reveno.atp.core.api.storage.SnapshotStorage.SnapshotStore;
import org.reveno.atp.core.channel.NettyBasedBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultSnapshotter implements RepositorySnapshotter {
	
	@Override
	public boolean hasAny() {
		return storage.getLastSnapshotStore() != null;
	}
	
	@Override
	public SnapshotIdentifier prepare() {
		return storage.nextSnapshotStore();
	}

	@Override
	public void snapshot(RepositoryData repo, SnapshotIdentifier identifier) {
		if (identifier.getType() != SnapshotStore.TYPE) {
			return;
		}
		SnapshotStore snap = (SnapshotStore) identifier;
		try (Channel c = storage.snapshotChannel(snap.getSnapshotPath())) {
			log.info("Performing default repository snapshot to " + snap);
			
			c.write(b -> repoSerializer.serialize(repo, b), true);
		} catch (Throwable t) {
			log.error("", t);
			throw new RuntimeException(t);
		}
	}

	@Override
	public RepositoryData load() {
		if (storage.getLastSnapshotStore() == null)
			return null;
		
		SnapshotStore snap = storage.getLastSnapshotStore();
		try (Channel c = storage.snapshotChannel(snap.getSnapshotPath())) {
			log.info("Loading repository snapshot from " + snap);

			return repoSerializer.deserialize(c.read());
		} catch (Throwable t) {
			log.error("", t);
			throw new RuntimeException(t);
		}
	}

	public DefaultSnapshotter(
			SnapshotStorage storage,
			RepositoryDataSerializer repoSerializer) {
		this.storage = storage;
		this.repoSerializer = repoSerializer;
	}

	
	protected final SnapshotStorage storage;
	protected final RepositoryDataSerializer repoSerializer;
	protected static final Logger log = LoggerFactory.getLogger(DefaultSnapshotter.class);

}
