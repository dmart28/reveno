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

package org.reveno.atp.api.dynamic;

import java.util.Map;

public class DynamicCommand {

	protected Class<? extends AbstractDynamicCommand> commandType;
	public Class<?> commandType() {
		return commandType;
	}
	
	protected Class<? extends AbstractDynamicTransaction> transactionType;
	public Class<?> transactionType() {
		return transactionType;
	}
	
	public AbstractDynamicCommand newCommand(Map<String, Object> args) throws InstantiationException, IllegalAccessException {
		return commandType.newInstance().args(args);
	}
	
	public DynamicCommand(Class<? extends AbstractDynamicCommand> commandType, Class<? extends AbstractDynamicTransaction> transactionType) {
		this.commandType = commandType;
		this.transactionType = transactionType;
	}
	
}
