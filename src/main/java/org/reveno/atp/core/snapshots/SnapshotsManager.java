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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.reveno.atp.api.RepositorySnapshooter;
import org.reveno.atp.core.api.serialization.RepositoryDataSerializer;
import org.reveno.atp.core.api.storage.SnapshotStorage;

public class SnapshotsManager {

	public RepositorySnapshooter defaultSnapshooter() {
		return snapshooters.get(DefaultSnapshooter.TYPE);
	}
	
	public void registerSnapshooter(RepositorySnapshooter snapshooter) {
		snapshooters.put(snapshooter.getType(), snapshooter);
	}
	
	@SuppressWarnings("serial")
	public void resetSnapshooters() {
		snapshooters = new LinkedHashMap<Long, RepositorySnapshooter>() {{
			put(DefaultSnapshooter.TYPE, defaultSnapshooter());
		}};
	}
	
	public void getSnapshooter(long type) {
		snapshooters.get(type);
	}
	
	public Collection<RepositorySnapshooter> getAll() {
		return snapshooters.values();
	}
	
	
	public SnapshotsManager(SnapshotStorage storage, RepositoryDataSerializer repoSerializer) {
		snapshooters.put(DefaultSnapshooter.TYPE, new DefaultSnapshooter(storage, repoSerializer));
	}
	
	
	protected volatile Map<Long, RepositorySnapshooter> snapshooters = new LinkedHashMap<>();
	
}
