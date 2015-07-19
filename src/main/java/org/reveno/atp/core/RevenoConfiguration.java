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
	public SnapshotConfiguration snapshotting() {
		return revenoSnapshotting();
	}
	public RevenoSnapshotConfiguration revenoSnapshotting() {
		return snapshotting;
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

    @Override
    public void preallocationSize(long size) {
        if (Long.bitCount(size) != 1) {
            throw new IllegalArgumentException("Files must have size of power of 2!");
        }
        this.preallocationSize = size;
    }
    public long preallocationSize() {
        return this.preallocationSize;
    }

    public CpuConsumption cpuConsumption() {
		return cpuConsumption;
	}
	
	protected RevenoSnapshotConfiguration snapshotting = new RevenoSnapshotConfiguration();
	protected CpuConsumption cpuConsumption = CpuConsumption.PHASED;
	protected ModelType modelType = ModelType.IMMUTABLE;
    protected long preallocationSize = 0L;
	
	public static class RevenoSnapshotConfiguration implements SnapshotConfiguration {

		@Override
		public void snapshotAtShutdown(boolean takeSnapshot) {
			this.snapshotAtShutdown = takeSnapshot;
		}
		public boolean snapshotAtShutdown() {
			return snapshotAtShutdown;
		}

		@Override
		public void snapshotEvery(long transactionCount) {
			this.snapshotEvery = transactionCount;
		}
		public long snapshotEvery() {
			return snapshotEvery;
		}
		
		private boolean snapshotAtShutdown = false;
		private long snapshotEvery = -1;
		
	}
	
}
