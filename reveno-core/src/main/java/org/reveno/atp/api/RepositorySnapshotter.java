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

package org.reveno.atp.api;

import org.reveno.atp.api.domain.RepositoryData;
import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.api.storage.SnapshotStorage;

public interface RepositorySnapshotter {

	default boolean hasAny() {
		return lastSnapshot() != null;
	}

	/**
	 * Returns last available snapshot.
	 * 
	 * @return
	 */
	SnapshotIdentifier lastSnapshot();

	/**
	 * Returns last version of Journal, after which snapshot was made. It was
	 * provided on the subsequent {@link #commit(long, SnapshotIdentifier)} method.
	 *
	 * It should return -1 if no snapshot had been made yet by this snapshotter.
	 *
	 * @return
     */
	long lastJournalVersionSnapshotted();
	
	/**
	 * Prepares SnapshotIdentifier pointer, using which snapshot
	 * can be written in particular way. It should be noted that this identifier
	 * should point for some temporary place, so, in case {@link #commit(long, SnapshotIdentifier)} is
	 * never called, this snapshot wouldn't affect engine replays at all, in other words, not used
	 * at all.
	 * 
	 * @return
	 */
	SnapshotIdentifier prepare();

	/**
	 * Performs actual snapshotting of {@link RepositoryData}. It still won't be available to
	 * the system for replay until {@link #commit(long, SnapshotIdentifier)} method is called.
	 *
	 * @param repo latest state of domain model
	 * @param identifier the result of previously called {@link #prepare()} method call
	 */
	void snapshot(RepositoryData repo, SnapshotIdentifier identifier);

	/**
	 * Commits snapshot @{code identifier} - makes it available for engine to replay.
	 *
	 * @param identifier
     */
	void commit(long lastJournalVersion, SnapshotIdentifier identifier);
	
	/**
	 * Loads last snapshot into {@link RepositoryData}
	 * 
	 * @return snapshotted state of domain model
	 */
	RepositoryData load();
	
	
	interface SnapshotIdentifier {
		
		byte getType();
		
		long getTime();

	}
	
}
