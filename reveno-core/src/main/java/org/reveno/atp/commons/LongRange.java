package org.reveno.atp.commons;

import java.util.Iterator;
import java.util.Objects;

/**
 * Range of two long numbers. It is used for replaying process, to mark unprocessed
 * gaps in events commits log files.
 *
 * @author Artem Dmitriev <art.dm.ser@gmail.com>
 */
public final class LongRange implements Iterable<Long>, Comparable<LongRange> {
	private static final LongRange[] EMPTY = new LongRange[0];
	public final long start;
	public final long end;

	public LongRange(long start, long end) {
		this.start = start;
		this.end = end;
	}
	
	public LongRange(long value) {
		this.start = value;
		this.end = value;
	}
	
	public boolean higher(long value) {
		return value > end;
	}

	public boolean contains(long value) {
		if (start == end)
			return value == start;
		return value >= start && value <= end;
	}
	
	public LongRange[] split(long value) {
		if (start == end) {
			return EMPTY;
		}
		if (value > start && value < end)
			return new LongRange[] { new LongRange(start, value - 1), new LongRange(value + 1, end) };
		else if (value == start) 
			return new LongRange[] { new LongRange(start + 1, end) };
		else if (value == end)
			return new LongRange[] { new LongRange(start, end - 1)};
		
		return null;
	}

	public Iterator<Long> iterator() {
		return new RangeIterator();
	}

	private final class RangeIterator implements Iterator<Long> {
		private long value;

		public RangeIterator() {
			value = start;
		}

		public boolean hasNext() {
			return value < end;
		}

		public Long next() {
			final long out = value;
			value += 1;
			return out;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public String toString() {
		return String.format("[%d;%d]", start, end);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		LongRange longs = (LongRange) o;
		return start == longs.start &&
				end == longs.end;
	}

	@Override
	public int hashCode() {
		return Objects.hash(start, end);
	}

	@Override
	public int compareTo(LongRange o) {
		return Long.compare(this.start, o.start);
	}
}
