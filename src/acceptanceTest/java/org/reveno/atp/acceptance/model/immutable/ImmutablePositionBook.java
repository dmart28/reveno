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

package org.reveno.atp.acceptance.model.immutable;

import static org.reveno.atp.acceptance.utils.Immutable.copy;
import static org.reveno.atp.acceptance.utils.Immutable.ma;
import static org.reveno.atp.acceptance.utils.Immutable.mr;
import static org.reveno.atp.acceptance.utils.Immutable.pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.reveno.atp.acceptance.model.Fill;
import org.reveno.atp.acceptance.model.Position;
import org.reveno.atp.acceptance.model.PositionBook;

public class ImmutablePositionBook implements PositionBook {

	private final HashMap<Long, Position> positions;
	
	public Map<Long, Position> positions() {
		return positions;
	}
	
	public ImmutablePositionBook addPosition(long id, String symbol, Fill fill) {
		return copy(this, pair("positions", ma(positions, id, ImmutablePosition.FACTORY.create(id, symbol, fill.accountId(), fill))));
	}
	
	public ImmutablePositionBook applyFill(Fill fill) {
		long positionId = fill.positionId();
		return copy(this, pair("positions", ma(positions, positionId, positions.get(positionId).addFill(fill))));
	}
	
	public ImmutablePositionBook merge(long toPositionId, List<Long> mergedPositions) {
		Position toPosition = positions.get(toPositionId);
		List<Fill> newFills = mergedPositions.stream().map(pid -> positions.get(pid))
				.flatMap(p -> p.fills().stream()).collect(Collectors.toList());
		HashMap<Long, Position> newPos = ma(positions, toPositionId, toPosition.addFills(newFills));
		mergedPositions.forEach(p -> newPos.remove(p));
		
		return copy(this, pair("positions", newPos));
	}
	
	public ImmutablePositionBook exit(long positionId) {
		Position position = positions.get(positionId);
		if (!position.isComplete())
			throw new RuntimeException("Can't exit not full filled position!");
		
		return copy(this, pair("positions", mr(positions, positionId)));
	}
	
	public ImmutablePositionBook() {
		this(new HashMap<>());
	}
	
	public ImmutablePositionBook(HashMap<Long, Position> positions) {
		this.positions = positions;
	}

	@Override
	public boolean isImmutable() {
		return true;
	}
	
}
