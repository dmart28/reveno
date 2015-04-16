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

import java.util.List;

import org.reveno.atp.api.events.EventBus;
import org.reveno.atp.core.api.EventsCommitInfo;
import org.reveno.atp.core.api.Journaler;
import org.reveno.atp.core.api.TransactionCommitInfo;
import org.reveno.atp.core.api.TxRepository;
import org.reveno.atp.core.api.serialization.TransactionInfoSerializer;
import org.reveno.atp.core.engine.components.CommandsManager;
import org.reveno.atp.core.engine.components.TransactionsManager;

public interface WorkflowContext {
	
	public List<TransactionInfoSerializer> serializers();
	
	public TransactionsManager transactionsManager();
	
	public CommandsManager commandsManager();
	
	public EventBus eventBus();
	
	public TransactionCommitInfo.Builder transactionCommitBuilder();
	
	public Journaler transactionJournaler();
	
	public Journaler eventsJournaler();
	
	public EventsCommitInfo.Builder eventsCommitBuilder();
	
	public long nextTransactionId();
	
	public TxRepository repository();
	
}
