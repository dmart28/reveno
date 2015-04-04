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

package org.reveno.atp.core.restore;

import org.reveno.atp.core.api.SystemStateRestorer;
import org.reveno.atp.core.api.storage.JournalsStorage;
import org.reveno.atp.core.api.storage.SnapshotStorage;

public class DefaultSystemStateRestorer implements SystemStateRestorer {

	@Override
	public SystemState restore() {
		return new SystemState(null, 0L);
	}
	
	
	public DefaultSystemStateRestorer(JournalsStorage journalStorage, SnapshotStorage snapshotStorage) {
		this.journalStorage = journalStorage;
		this.snapshotStorage = snapshotStorage;
	}
	
	
	protected final JournalsStorage journalStorage;
	protected final SnapshotStorage snapshotStorage;

}
