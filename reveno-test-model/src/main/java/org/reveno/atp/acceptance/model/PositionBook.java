package org.reveno.atp.acceptance.model;

import java.util.List;
import java.util.Map;

public interface PositionBook extends Changeable {

    Map<Long, Position> positions();

    PositionBook addPosition(long id, String symbol, Fill fill);

    PositionBook applyFill(Fill fill);

    PositionBook merge(long toPositionId, List<Long> mergedPositions);

    PositionBook exit(long positionId);


    public interface PositionBookFactory {
        PositionBook create();
    }

}
