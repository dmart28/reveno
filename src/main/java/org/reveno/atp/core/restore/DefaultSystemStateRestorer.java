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

import java.util.function.Consumer;

import org.reveno.atp.api.RepositorySnapshooter;
import org.reveno.atp.core.EngineWorkflowContext;
import org.reveno.atp.core.api.SystemStateRestorer;
import org.reveno.atp.core.api.TxRepository;
import org.reveno.atp.core.api.TxRepositoryFactory;
import org.reveno.atp.core.api.storage.JournalsStorage;
import org.reveno.atp.core.engine.WorkflowEngine;
import org.reveno.atp.core.snapshots.SnapshotsManager;

public class DefaultSystemStateRestorer implements SystemStateRestorer {

	@Override
	public void restore(Consumer<SystemState> handler) {
		TxRepository repository = combineRepository();
		
		// TODO further implementation
	}

	
	protected TxRepository combineRepository() {
		if (snapshooter().hasAny()) {
			return repoFactory.create(snapshooter().load());
		} else return repoFactory.create();
	}
	
	protected RepositorySnapshooter snapshooter() {
		return snapshotsManager.defaultSnapshooter();
	}
	
	
	public DefaultSystemStateRestorer(JournalsStorage journalStorage,
			TxRepositoryFactory repoFactory,
			SnapshotsManager snapshotsManager,
			EngineWorkflowContext workflowContext,
			WorkflowEngine workflowEngine) {
		this.journalStorage = journalStorage;
		this.repoFactory = repoFactory;
		this.snapshotsManager = snapshotsManager;
		this.workflowContext = workflowContext;
		this.workflowEngine = workflowEngine;
	}
	
	protected final JournalsStorage journalStorage;
	protected final TxRepositoryFactory repoFactory;
	protected final EngineWorkflowContext workflowContext;
	protected final WorkflowEngine workflowEngine;
	protected final SnapshotsManager snapshotsManager;
	
}
