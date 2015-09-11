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

public interface Configuration {
	
	SnapshotConfiguration snapshotting();
	
	DisruptorConfiguration disruptor();

	JournalingConfiguration journaling();


	Configuration mutableModel();

	Configuration immutableModel();

	Configuration mutableModelFailover(MutableModelFailover mutableModelFailover);

	Configuration cpuConsumption(CpuConsumption cpuConsumption);
	
    
	public static interface SnapshotConfiguration {
		SnapshotConfiguration atShutdown(boolean takeSnapshot);

		SnapshotConfiguration every(long transactionCount);
	}
	
	public static interface DisruptorConfiguration {
		DisruptorConfiguration bufferSize(int bufferSize);
	}

	public static interface JournalingConfiguration {

		JournalingConfiguration maxObjectSize(int size);

		JournalingConfiguration volumesSize(long txSize, long eventsSize);

		JournalingConfiguration volumes(int volumes);

		JournalingConfiguration minVolumes(int volumes);

		JournalingConfiguration channelOptions(ChannelOptions options);

	}
	
	public static enum ModelType { MUTABLE, IMMUTABLE }
	
	public static enum MutableModelFailover { SNAPSHOTS, COMPENSATING_ACTIONS}
	
	public static enum CpuConsumption { LOW, NORMAL, HIGH, PHASED }


	default void modelType(ModelType modelType) {
		switch (modelType) {
			case MUTABLE: mutableModel(); break;
			case IMMUTABLE: immutableModel(); break;
		}
	}
	
}
