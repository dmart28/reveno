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

package org.reveno.atp.acceptance.model;

import java.util.List;
import java.util.Set;

public interface Account extends Changeable {
	
	long id();
	
	String currency();
	
	long balance();
	
	Set<Long> orders();
	
	PositionBook positions();

	Account addBalance(long add);
	
	Account addOrder(long orderId);
	
	Account removeOrder(long orderId);
	
	Account addPosition(long id, String symbol, Fill fill);
	
	Account applyFill(Fill fill);
	
	Account mergePositions(long toPositionId, List<Long> mergedPositions);
	
	Account exitPosition(long positionId, long pnl);
	
	
	public interface AccountFactory {	
		Account create(long id, String currency, long balance);
	}
	
}
