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

package org.reveno.atp.acceptance.model.mutable;

import java.util.ArrayList;
import java.util.Collection;

public class Position {

	public final long id;
	public final String symbol;
	public final long accountId;
	public ArrayList<Fill> fills = new ArrayList<>();
	
	public void addFill(Fill fill) {
		this.fills.add(fill);
	}
	
	public void addFills(Collection<Fill> fills) {
		this.fills.addAll(fills);
	}
	
	public boolean buy() {
		return sum() > 0;
	}
	
	public boolean isComplete() {
		return sum() == 0L;
	}

	public long sum() {
		return fills.stream().mapToLong(f -> f.size).sum();
	}
	
	public Position(long id, String symbol, long accountId, Fill fill) {
		this.id = id;
		this.symbol = symbol;
		this.accountId = accountId;
		this.fills.add(fill);
	}
	
}
