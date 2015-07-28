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

package org.reveno.atp.core.engine;

import org.reveno.atp.core.JournalsRoller;
import org.reveno.atp.core.RevenoConfiguration;
import org.reveno.atp.core.api.*;
import org.reveno.atp.core.engine.components.CommandsManager;
import org.reveno.atp.core.engine.components.SerializersChain;
import org.reveno.atp.core.engine.components.TransactionsManager;
import org.reveno.atp.core.events.EventPublisher;
import org.reveno.atp.core.snapshots.SnapshottersManager;
import org.reveno.atp.core.views.ViewsProcessor;

public interface WorkflowContext {
	
	public RevenoConfiguration configuration();
	
	
	public IdGenerator idGenerator();
	
	public SerializersChain serializer();
	
	public TxRepository repository();
	
	public ViewsProcessor viewsProcessor();
	
	public TransactionsManager transactionsManager();
	
	public CommandsManager commandsManager();
	
	public InterceptorCollection interceptorCollection();
	
	
	public EventPublisher eventPublisher();
	
	
	public TransactionCommitInfo.Builder transactionCommitBuilder();
	
	public Journaler transactionJournaler();
	
	public JournalsRoller roller();
	
	
	public SnapshottersManager snapshotsManager();
	
}
