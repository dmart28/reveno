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

package org.reveno.atp.api.transaction;

import org.reveno.atp.api.domain.WriteableRepository;

/*
 * Allowes you to intercept into transaction processing.
 * 
 * WARNING: it's some kind of dangerous field, and it should be used with
 * super care, since inappropriate usage might be extremely harmful.
 * 
 * Good practice is to use it for some statistics gathering, snapshotting logic, etc.
 * 
 */
@FunctionalInterface
public interface TransactionInterceptor {

	void intercept(long transactionId, long time, WriteableRepository repository, TransactionStage stage);
	
}
