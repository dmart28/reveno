package org.reveno.atp.acceptance.model.immutable;

import org.reveno.atp.acceptance.model.Fill;
import org.reveno.atp.acceptance.model.Position;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ImmutablePosition implements Position {

    public static final PositionFactory FACTORY = new PositionFactory() {
        @Override
        public Position create(long id, String symbol, long accountId,
                               Fill fill) {
            ImmutablePosition p = new ImmutablePosition(id, symbol, accountId, new ArrayList<>());
            p.fills.add(fill);
            return p;
        }
    };
    private final long id;
    private final String symbol;
    private final long accountId;
    private final ArrayList<Fill> fills;

    public ImmutablePosition(long id, String symbol, long accountId, ArrayList<Fill> fills) {
        this.id = id;
        this.symbol = symbol;
        this.accountId = accountId;
        this.fills = fills;
    }

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
        return Collections.unmodifiableList(fills);
    }

    public ImmutablePosition addFill(Fill fill) {
        ArrayList<Fill> newFills = new ArrayList<>(fills);
        newFills.add(fill);
        return new ImmutablePosition(id, symbol, accountId, newFills);
    }

    public ImmutablePosition addFills(Collection<Fill> fills) {
        ArrayList<Fill> newFills = new ArrayList<>(fills);
        newFills.addAll(fills);
        return new ImmutablePosition(id, symbol, accountId, newFills);
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

    @Override
    public boolean isImmutable() {
        return true;
    }

}
