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

import java.util.ArrayList;
import java.util.List;

import org.reveno.atp.api.transaction.TransactionInterceptor;

public class InterceptorCollection {

	protected List<TransactionInterceptor> beforeInterceptors;
	public List<TransactionInterceptor> getBeforeInterceptors() {
		return beforeInterceptors;
	}
	
	protected List<TransactionInterceptor> afterInterceptors;
	public List<TransactionInterceptor> getAfterInterceptors() {
		return afterInterceptors;
	}
	
	public InterceptorCollection() {
		beforeInterceptors = new ArrayList<>();
		afterInterceptors = new ArrayList<>();
	}
	
	public InterceptorCollection(List<TransactionInterceptor> beforeInterceptors, 
			List<TransactionInterceptor> afterInterceptors) {
		this.beforeInterceptors = beforeInterceptors;
		this.afterInterceptors = afterInterceptors;
	}
	
}
