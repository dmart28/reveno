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
import java.util.List;

import org.reveno.atp.acceptance.model.Fill;
import org.reveno.atp.acceptance.model.Position;

public class MutablePosition implements Position {

	private final long id;
	private final String symbol;
	private final long accountId;
	private ArrayList<Fill> fills = new ArrayList<>();
	
	public long id() {
		return id;
	}
	
	public String symbol() {
		return symbol;
	}
	
	public long accountId() {
		return accountId;
	}
	
	public List<Fill> fills() {
		return fills;
	}
	
	public Position addFill(Fill fill) {
		this.fills.add(fill);
		return this;
	}
	
	public Position addFills(Collection<Fill> fills) {
		this.fills.addAll(fills);
		return this;
	}
	
	public boolean buy() {
		return sum() > 0;
	}
	
	public boolean isComplete() {
		return sum() == 0L;
	}

	public long sum() {
		return fills.stream().mapToLong(f -> f.size()).sum();
	}
	
	public MutablePosition(long id, String symbol, long accountId, Fill fill) {
		this.id = id;
		this.symbol = symbol;
		this.accountId = accountId;
		this.fills.add(fill);
	}

	@Override
	public boolean isImmutable() {
		return false;
	}
	
	public static final PositionFactory FACTORY = new PositionFactory() {
		@Override
		public Position create(long id, String symbol, long accountId, Fill fill) {
			return new MutablePosition(id, symbol, accountId, fill);
		}
	};
	
}
