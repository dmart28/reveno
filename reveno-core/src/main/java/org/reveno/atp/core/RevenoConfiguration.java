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

import org.reveno.atp.api.ChannelOptions;
import org.reveno.atp.api.Configuration;

import static org.reveno.atp.utils.MeasureUtils.kb;

public class RevenoConfiguration implements Configuration {

	@Override
	public SnapshotConfiguration snapshotting() {
		return revenoSnapshotting();
	}
	public RevenoSnapshotConfiguration revenoSnapshotting() {
		return snapshotting;
	}
	
	@Override
	public DisruptorConfiguration disruptor() {
		return revenoDisruptor();
	}
	public RevenoDisruptorConfiguration revenoDisruptor() {
		return disruptor;
	}

	@Override
	public JournalingConfiguration journaling() {
		return journaling;
	}

	@Override
	public Configuration mutableModel() {
		this.modelType = ModelType.MUTABLE;
		return this;
	}

	@Override
	public Configuration immutableModel() {
		this.modelType = ModelType.IMMUTABLE;
		return this;
	}

	public ModelType modelType() {
		return modelType;
	}

	public RevenoJournalingConfiguration revenoJournaling() {
		return journaling;
	}

	@Override
	public Configuration mutableModelFailover(MutableModelFailover mutableModelFailover) {
		this.mutableModelFailover = mutableModelFailover;
		return this;
	}
	public MutableModelFailover mutableModelFailover() {
		return mutableModelFailover;
	}

	@Override
	public Configuration cpuConsumption(CpuConsumption cpuConsumption) {
		this.cpuConsumption = cpuConsumption;
		return this;
	}

	@Override
	public void mapCapacity(int capacity) {
		this.mapCapacity = capacity;
	}
	public int mapCapacity() {
		return mapCapacity;
	}

	@Override
	public void mapLoadFactor(float loadFactor) {
		this.mapLoadFactor = loadFactor;
	}
	public float mapLoadFactor() {
		return mapLoadFactor;
	}

	public CpuConsumption cpuConsumption() {
		return cpuConsumption;
	}
	
	protected RevenoSnapshotConfiguration snapshotting = new RevenoSnapshotConfiguration();
	protected RevenoDisruptorConfiguration disruptor = new RevenoDisruptorConfiguration();
	protected RevenoJournalingConfiguration journaling = new RevenoJournalingConfiguration();
	protected CpuConsumption cpuConsumption = CpuConsumption.NORMAL;
	protected ModelType modelType = ModelType.IMMUTABLE;
	protected int mapCapacity = 524288;
	protected float mapLoadFactor = 0.75f;
	protected MutableModelFailover mutableModelFailover = MutableModelFailover.SNAPSHOTS;

	
	public static class RevenoSnapshotConfiguration implements SnapshotConfiguration {

		@Override
		public SnapshotConfiguration atShutdown(boolean takeSnapshot) {
			this.snapshotAtShutdown = takeSnapshot;
			return this;
		}
		public boolean atShutdown() {
			return snapshotAtShutdown;
		}

		@Override
		public SnapshotConfiguration every(long transactionCount) {
			this.snapshotEvery = transactionCount;
			return this;
		}
		public long every() {
			return snapshotEvery;
		}
		
		private boolean snapshotAtShutdown = false;
		private long snapshotEvery = -1;
		
	}
	
	public static class RevenoDisruptorConfiguration implements DisruptorConfiguration {

		@Override
		public DisruptorConfiguration bufferSize(int bufferSize) {
			if (Integer.bitCount(bufferSize) != 1) {
				throw new IllegalArgumentException("Disruptor buffer size must be of power of 2.");
			}
			this.bufferSize = bufferSize;
			return this;
		}
		public int bufferSize() {
			return bufferSize;
		}
		
		private int bufferSize = 1024;
		
	}

	public static class RevenoJournalingConfiguration implements JournalingConfiguration {

		@Override
		public JournalingConfiguration maxObjectSize(int size) {
			if (maxObjectSize < MIN_MAX_OBJECT_SIZE)
				throw new IllegalArgumentException(String.format("Max object size can't be less than %s bytes.", MIN_MAX_OBJECT_SIZE));
			this.maxObjectSize = size;
			return this;
		}
		public int maxObjectSize() {
			return maxObjectSize;
		}

		@Override
		public JournalingConfiguration volumesSize(long txSize, long eventsSize) {
			this.txSize = txSize;
			this.eventsSize = eventsSize;
			return this;
		}
		public long txSize() {
			return txSize;
		}
		public long eventsSize() {
			return eventsSize;
		}
		public boolean isPreallocated() {
			return txSize != 0 || eventsSize != 0;
		}

		@Override
		public JournalingConfiguration volumes(int volumes) {
			this.volumes = volumes;
			return this;
		}
		public int volumes() {
			return volumes;
		}

		@Override
		public JournalingConfiguration minVolumes(int volumes) {
			this.minVolumes = volumes;
			return this;
		}
		public int minVolumes() {
			return minVolumes;
		}

		@Override
		public JournalingConfiguration channelOptions(ChannelOptions options) {
			this.channelOptions = options;
			return this;
		}
		public ChannelOptions channelOptions() {
			return channelOptions;
		}

		protected long txSize = 0L, eventsSize = 0L;
		protected int volumes = 3;
		protected int minVolumes = 1;
		protected int maxObjectSize = kb(128);
		protected ChannelOptions channelOptions = ChannelOptions.BUFFERING_VM;

		protected static final int MIN_MAX_OBJECT_SIZE = 64;
	}
	
}
