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

package org.reveno.atp.core;

import org.reveno.atp.api.Configuration;

public class RevenoConfiguration implements Configuration {

	@Override
	public SnapshotConfiguration snapshooting() {
		return revenoSnapshooting();
	}
	public RevenoSnapshotConfiguration revenoSnapshooting() {
		return snapshooting;
	}

	@Override
	public void modelType(ModelType modelType) {
		this.modelType = modelType;
	}
	public ModelType modelType() {
		return modelType;
	}

	@Override
	public void cpuConsumption(CpuConsumption cpuConsumption) {
		this.cpuConsumption = cpuConsumption;
	}
	public CpuConsumption cpuConsumption() {
		return cpuConsumption;
	}
	
	private RevenoSnapshotConfiguration snapshooting = new RevenoSnapshotConfiguration();
	private CpuConsumption cpuConsumption = CpuConsumption.LOW;
	private ModelType modelType = ModelType.IMMUTABLE;
	
	
	protected static class RevenoSnapshotConfiguration implements SnapshotConfiguration {

		@Override
		public void snapshootAtShutdown(boolean takeSnapshot) {
			this.snapshootAtShutdown = takeSnapshot;
		}
		public boolean snapshootAtShutdown() {
			return snapshootAtShutdown;
		}
		
		@Override
		public void snapshootOnException(boolean snapshootOnException) {
			this.snapshootOnException = snapshootOnException;
		}
		public boolean snapshootOnException() {
			return snapshootOnException;
		}

		@Override
		public void snapshootWithInterval(long interval) {
			this.snapshootWithInterval = interval;
		}
		public long snapshootWithInterval() {
			return snapshootWithInterval;
		}

		@Override
		public void snapshootEvery(long transactionCount) {
			this.snapshootEvery = transactionCount;
		}
		public long snapshootEvery() {
			return snapshootEvery;
		}
		
		private volatile boolean snapshootAtShutdown = false;
		private volatile boolean snapshootOnException = false;
		private volatile long snapshootWithInterval = -1;
		private volatile long snapshootEvery = -1;
		
	}
	
}
