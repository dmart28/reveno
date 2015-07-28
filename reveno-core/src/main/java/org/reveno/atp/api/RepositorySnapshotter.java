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
import org.reveno.atp.core.api.storage.SnapshotStorage;

/**
 * Snapshotter is the instrument 
 * 
 * @author Artem Dmitriev <art.dm.ser@gmail.com>
 *
 */
public interface RepositorySnapshotter {

	/**
	 * Checks is there any available snapshot to be loaded.
	 * 
	 * @return if any snapshot available
	 */
	boolean hasAny();
	
	/**
	 * Performs snapshotting of {@link RepositoryData} to some {@link SnapshotStorage}
	 * 
	 * @param repo latest state of domain model
	 */
	void snapshot(RepositoryData repo);
	
	/**
	 * Loads last snapshot into {@link RepositoryData}
	 * 
	 * @return snapshotted state of domain model
	 */
	RepositoryData load();
}
