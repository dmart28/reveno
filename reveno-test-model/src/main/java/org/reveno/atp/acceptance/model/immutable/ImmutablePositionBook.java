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

    public ImmutablePositionBook() {
        this(new HashMap<>());
    }

    public ImmutablePositionBook(HashMap<Long, Position> positions) {
        this.positions = positions;
    }

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
        List<Fill> newFills = mergedPositions.stream().map(positions::get)
                .flatMap(p -> p.fills().stream()).collect(Collectors.toList());
        HashMap<Long, Position> newPositions = new HashMap<>(positions);
        newPositions.put(toPositionId, toPosition.addFills(newFills));
        mergedPositions.forEach(newPositions::remove);

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

    @Override
    public boolean isImmutable() {
        return true;
    }

}
