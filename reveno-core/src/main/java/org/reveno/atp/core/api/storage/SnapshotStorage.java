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

package org.reveno.atp.core.api.storage;

import org.reveno.atp.api.RepositorySnapshotter.SnapshotIdentifier;
import org.reveno.atp.core.api.channel.Channel;

public interface SnapshotStorage {

	Channel snapshotChannel(String address);

	SnapshotStore getLastSnapshotStore();
	
	SnapshotStore nextSnapshotAfter(long lastJournalVersion);

	SnapshotStore nextTempSnapshotStore();

	void removeSnapshotStore(SnapshotStore snapshot);

	void move(SnapshotStore from, SnapshotStore to);
	
	void removeLastSnapshotStore();
	
	
	class SnapshotStore implements SnapshotIdentifier {
		public static final byte TYPE = 0x1;
		
		public byte getType() {
			return TYPE;
		}
		
		private String snapshotPath;
		public String getSnapshotPath() {
			return snapshotPath;
		}
		public void setSnapshotPath(String snapshotPath) {
			this.snapshotPath = snapshotPath;
		}
		
		private long snapshotTime;
		public long getTime() {
			return snapshotTime;
		}

		private long version;
		public long getVersion() {
			return version;
		}

		private long lastJournalVersion;
		public long getLastJournalVersion() {
			return lastJournalVersion;
		}
		
		public SnapshotStore(String path, long time, long version, long lastJournalVersion) {
			this.snapshotPath = path;
			this.snapshotTime = time;
			this.version = version;
			this.lastJournalVersion = lastJournalVersion;
		}
		
		@Override
		public String toString() {
			return String.format("[store:%s]", snapshotPath);
		}
	}
	
}
