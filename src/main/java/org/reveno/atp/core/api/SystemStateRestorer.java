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

package org.reveno.atp.core.api;

import org.reveno.atp.api.domain.RepositoryData;

public interface SystemStateRestorer {
	
	SystemState restore();
	
	
	public static class SystemState {
		private RepositoryData repositoryData;
		public RepositoryData getRepositoryData() {
			return repositoryData;
		}
		
		private long lastTransactionId;
		public long getLastTransactionId() {
			return lastTransactionId;
		}
		
		public SystemState(RepositoryData repositoryData, long lastTransactionId) {
			this.repositoryData = repositoryData;
			this.lastTransactionId = lastTransactionId;
		}
	}
	
}
