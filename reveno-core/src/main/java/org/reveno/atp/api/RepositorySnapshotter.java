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

/**
 * Snapshotter is the instrument 
 * 
 * @author Artem Dmitriev <art.dm.ser@gmail.com>
 *
 */
public interface RepositorySnapshotter {

	default boolean hasAny() {
		return lastSnapshotVersion() != -1;
	}

	/**
	 * Returns last available version of snapshot, -1 if no snapshot exists.
	 * 
	 * @return version of last committed snapshot available, -1 otherwise
	 */
	long lastSnapshotVersion();
	
	/**
	 * Prepares SnapshotIdentifier pointer, using which snapshot
	 * can be written in particular way. It should be noted that this identifier
	 * should point for some temporary place, so, in case {@link #commit(SnapshotIdentifier)} is
	 * never called, this snapshot wouldn't affect engine replays at all, in other words, not used
	 * at all.
	 * 
	 * @return
	 */
	SnapshotIdentifier prepare(long version);

	/**
	 * Performs snapshotting of {@link RepositoryData} to some {@link SnapshotStorage}
	 *
	 * @param repo latest state of domain model
	 * @param identifier the result of previously called {@link #prepare()} method call
	 */
	void snapshot(RepositoryData repo, SnapshotIdentifier identifier);

	/**
	 * Commits @{code identifier} - makes it available for engine state replay.
	 *
	 * @param identifier
     */
	void commit(SnapshotIdentifier identifier);
	
	/**
	 * Loads last snapshot into {@link RepositoryData}
	 * 
	 * @return snapshotted state of domain model
	 */
	RepositoryData load();
	
	
	interface SnapshotIdentifier {
		
		byte getType();
		
		long getTime();

		long getLastJournalVersion();

	}
	
}
