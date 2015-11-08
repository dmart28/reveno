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

import org.reveno.atp.acceptance.model.Fill;
import org.reveno.atp.acceptance.model.Position;
import org.reveno.atp.acceptance.model.PositionBook;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ImmutablePositionBook implements PositionBook {

	private final HashMap<Long, Position> positions;
	
	public Map<Long, Position> positions() {
		return positions;
	}
	
	public ImmutablePositionBook addPosition(long id, String symbol, Fill fill) {
		HashMap<Long, Position> newPositions = new HashMap<>(positions);
		newPositions.put(id, ImmutablePosition.FACTORY.create(id, symbol, fill.accountId(), fill));
		return new ImmutablePositionBook(newPositions);
	}
	
	public ImmutablePositionBook applyFill(Fill fill) {
		long positionId = fill.positionId();
		HashMap<Long, Position> newPositions = new HashMap<>(positions);
		newPositions.put(positionId, positions.get(positionId).addFill(fill));
		return new ImmutablePositionBook(newPositions);
	}
	
	public ImmutablePositionBook merge(long toPositionId, List<Long> mergedPositions) {
		Position toPosition = positions.get(toPositionId);
		List<Fill> newFills = mergedPositions.stream().map(pid -> positions.get(pid))
				.flatMap(p -> p.fills().stream()).collect(Collectors.toList());
		HashMap<Long, Position> newPositions = new HashMap<>(positions);
		newPositions.put(toPositionId, toPosition.addFills(newFills));
		mergedPositions.forEach(p -> newPositions.remove(p));
		
		return new ImmutablePositionBook(newPositions);
	}
	
	public ImmutablePositionBook exit(long positionId) {
		Position position = positions.get(positionId);
		if (!position.isComplete())
			throw new RuntimeException("Can't exit not full filled position!");
		
		HashMap<Long, Position> newPositions = new HashMap<>(positions);
		newPositions.remove(positionId);
		return new ImmutablePositionBook(newPositions);
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
