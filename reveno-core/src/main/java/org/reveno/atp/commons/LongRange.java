package org.reveno.atp.commons;

import java.util.Iterator;

/**
 * Range of two long numbers. It is used for replaying process, to mark unprocessed
 * gaps in events commits log files.
 *
 * @author Artem Dmitriev <art.dm.ser@gmail.com>
 */
public final class LongRange implements Iterable<Long>, Comparable<LongRange> {

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
		if (start == end)
			return new LongRange[0];
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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (end ^ (end >>> 32));
		result = prime * result + (int) (start ^ (start >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LongRange other = (LongRange) obj;
		if (end != other.end)
			return false;
		if (start != other.start)
			return false;
		return true;
	}

	@Override
	public int compareTo(LongRange o) {
		if (this.start > o.start) return 1;
		if (this.start < o.start) return -1;
		return 0;
	}
	
	public final long start;
	public final long end;
}
