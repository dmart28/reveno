package org.reveno.atp.acceptance.model.mutable;

import org.reveno.atp.acceptance.model.Fill;
import org.reveno.atp.acceptance.model.Position;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MutablePosition implements Position {

    public static final PositionFactory FACTORY = new PositionFactory() {
        @Override
        public Position create(long id, String symbol, long accountId, Fill fill) {
            return new MutablePosition(id, symbol, accountId, fill);
        }
    };
    private final long id;
    private final String symbol;
    private final long accountId;
    private ArrayList<Fill> fills = new ArrayList<>();

    public MutablePosition(long id, String symbol, long accountId, Fill fill) {
        this.id = id;
        this.symbol = symbol;
        this.accountId = accountId;
        this.fills.add(fill);
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

    @Override
    public boolean isImmutable() {
        return false;
    }

}
