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

import org.reveno.atp.api.transaction.TransactionInterceptor;
import org.reveno.atp.api.transaction.TransactionStage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InterceptorCollection {

	protected Map<TransactionStage, List<TransactionInterceptor>> interceptors = new HashMap<>();
	
	public List<TransactionInterceptor> getInterceptors(TransactionStage stage) {
		if (!interceptors.containsKey(stage))
			interceptors.put(stage, new ArrayList<>());
		return interceptors.get(stage);
	}
	
	public void add(TransactionStage stage, TransactionInterceptor interceptor) {
		getInterceptors(stage).add(interceptor);
	}
	
}
