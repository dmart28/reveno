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

package org.reveno.atp.commons;

import java.util.Iterator;

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
