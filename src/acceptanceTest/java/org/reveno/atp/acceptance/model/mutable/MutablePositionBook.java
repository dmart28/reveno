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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.reveno.atp.acceptance.model.Fill;
import org.reveno.atp.acceptance.model.Position;
import org.reveno.atp.acceptance.model.PositionBook;

public class MutablePositionBook implements PositionBook {

	private final HashMap<Long, Position> positions = new HashMap<>();
	
	public Map<Long, Position> positions() {
		return positions;
	}
	
	public PositionBook addPosition(long id, String symbol, Fill fill) {
		this.positions.put(id, new MutablePosition(id, symbol, fill.accountId(), fill));
		return this;
	}
	
	public PositionBook applyFill(Fill fill) {
		long positionId = fill.positionId();
		this.positions.get(positionId).addFill(fill);
		return this;
	}
	
	public PositionBook merge(long toPositionId, List<Long> mergedPositions) {
		List<Fill> newFills = mergedPositions.stream().map(pid -> positions.get(pid))
				.flatMap(p -> p.fills().stream()).collect(Collectors.toList());
		positions.get(toPositionId).addFills(newFills);
		mergedPositions.forEach(p -> positions.remove(p));
		return this;
	}
	
	public PositionBook exit(long positionId) {
		Position position = positions.get(positionId);
		if (!position.isComplete())
			throw new RuntimeException("Can't exit not full filled position!");
		
		positions.remove(positionId);
		return this;
	}
	
	@Override
	public boolean isImmutable() {
		return false;
	}
	
	public static final PositionBookFactory FACTORY = new PositionBookFactory() {
		@Override
		public PositionBook create() {
			return new MutablePositionBook();
		}
	};
	
}
