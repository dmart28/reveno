package org.reveno.atp.metrics.meter.impl;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

import org.reveno.atp.metrics.meter.Histogram;
import org.reveno.atp.metrics.meter.HistogramType;

public class TwoBufferHistogram implements Histogram {
	
	protected static final int BITS_PER_LONG = 63;
	protected static final byte WRITING = 1;
	protected static final byte NEED_RESET = 2;
	protected static final byte RESET_DONE = 3;

	@Override
	public Map<String, String> snapshot() {
		return null;
	}
	
	public void switchNext() {
		int nextIndex = (int) (switcher.incrementAndGet() % bufferCount);
		bit = (nextIndex << 2) & NEED_RESET; 
		for (;;) {
			if ((bit << 30) >> 30 == RESET_DONE)
				break;
			else
				Thread.yield();
		}
	}

	@Override
	public void update(long value, long time) {
		int currentIndex = bit >> 2;
		ByteBuffer cur = bufs[currentIndex];
		if ((bit << 30) >> 30 == NEED_RESET) {
			bit = currentIndex << 2 & RESET_DONE;
		}
		if (cur.remaining() >= 16) {
			cur.putLong(value);
			cur.putLong(time);
		} else if (type == HistogramType.RANDOM_VITTERS_R) {
			long pos = cur.position();
			
		}
	}
	
	@Override
	public boolean isReady() {
		return bufs[bit >> 2].remaining() < 16;
	}
	
	protected int currentIndex() {
		return (int) (switcher.get() & bufferCount);
	}
	
	protected int prevIndex() {
		if (currentIndex() == 1) return 0;
		else return 1;
	}
	
	/**
     * Get a pseudo-random long uniformly between 0 and n-1. Stolen from
     * {@link java.util.Random#nextInt()}.
     *
     * @param n the bound
     * @return a value select randomly from the range {@code [0..n)}.
     */
    protected static long nextLong(long n) {
        long bits, val;
        do {
            bits = ThreadLocalRandom.current().nextLong() & (~(1L << BITS_PER_LONG));
            val = bits % n;
        } while (bits - val + (n - 1) < 0L);
        return val;
    }

	public TwoBufferHistogram(int bufferSize, HistogramType type) {
		if (Integer.bitCount(bufferSize) != 1) {
			throw new IllegalArgumentException("Buffer size must be pow(2, n) number!");
		}
		this.bufferSize = bufferSize;
		this.bufs = new ByteBuffer[bufferCount];
		for (int i = 0; i < bufferCount; i++) {
			this.bufs[i] = ByteBuffer.allocateDirect(bufferSize);
		}
		this.type = type;
	}
	
	protected volatile int bit = WRITING;
	protected final ByteBuffer[] bufs;
	protected final int bufferSize;
	protected final int bufferCount = 2;
	protected final AtomicLong switcher = new AtomicLong();
	protected final HistogramType type;
	
}
