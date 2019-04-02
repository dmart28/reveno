package org.reveno.atp.acceptance.model.mutable;

import org.reveno.atp.acceptance.model.Fill;
import org.reveno.atp.acceptance.model.Position;
import org.reveno.atp.acceptance.model.PositionBook;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
