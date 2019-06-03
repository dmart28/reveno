package org.reveno.atp.metrics.meter.impl;

import org.reveno.atp.metrics.Sink;
import org.reveno.atp.metrics.meter.Histogram;
import org.reveno.atp.metrics.meter.HistogramType;
import org.reveno.atp.utils.UnsafeUtils;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public class TwoBufferHistogram implements Histogram {

    protected static final int BITS_PER_LONG = 63;
    protected static final int BYTES_PER_LONG = 8;
    protected static final byte WRITING = 1;
    protected static final byte NEED_RESET = 2;
    protected static final byte RESET_DONE = 3;
    protected final ByteBuffer[] bufs;
    protected final int bufferSize;
    protected final int bufferCount = 2;
    protected final AtomicLong switcher = new AtomicLong();
    protected final HistogramType type;
    protected final String name;
    protected final String countName;
    protected final String meanName;
    protected final String minName;
    protected final String maxName;
    protected final String stddevName;
    protected long prevMean = -1L;
    protected LongAdder count = new LongAdder();
    protected volatile int bit = WRITING;
    public TwoBufferHistogram(String name, int bufferSize) {
        this(name, bufferSize, HistogramType.RANDOM_VITTERS_R);
    }
    public TwoBufferHistogram(String name, int bufferSize, HistogramType type) {
        if (Integer.bitCount(bufferSize) != 1) {
            throw new IllegalArgumentException("Buffer size must be pow(2, n) number!");
        }
        this.bufferSize = bufferSize;
        this.bufs = new ByteBuffer[bufferCount];
        for (int i = 0; i < bufferCount; i++) {
            this.bufs[i] = ByteBuffer.allocateDirect(bufferSize);
        }
        this.type = type;
        this.name = name;
        this.countName = name + ".count";
        this.meanName = name + ".mean";
        this.minName = name + ".min";
        this.maxName = name + ".max";
        this.stddevName = name + ".stddev";
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

    @Override
    public void sendTo(List<Sink> sinks, boolean sync) {
        switchNext(sync);
        ByteBuffer buffer = this.bufs[prevIndex()];

        long sum = count.sumThenReset();
        long amount = Math.min(sum, buffer.limit() / BYTES_PER_LONG);
        long timestamp = System.currentTimeMillis() / 1000;
        buffer.clear();

        long mean = 0;
        long min = Long.MAX_VALUE;
        long max = 0;
        long stddev = 0;

        int count = 0;
        while (buffer.position() <= buffer.limit() && (count += 1) <= amount) {
            final long metric = buffer.getLong();
            mean += metric;
            if (metric > max) {
                max = metric;
            }
            if (metric < min) {
                min = metric;
            }

            if (prevMean != -1) {
                stddev += Math.pow(prevMean - metric, 2);
            }
        }
        mean /= count - 1;
        if (prevMean == -1) {
            stddev = 0;
        } else {
            stddev /= count;
            stddev = (long) Math.sqrt(stddev);
        }
        prevMean = mean;

        for (Sink sink : sinks) {
            sink.send(countName, Long.toString(sum), timestamp);
            sink.send(meanName, Long.toString(mean), timestamp);
            sink.send(minName, Long.toString(min), timestamp);
            sink.send(maxName, Long.toString(max), timestamp);
            sink.send(stddevName, Long.toString(stddev), timestamp);
        }

        buffer.clear();
    }

    public void switchNext(boolean sync) {
        int nextIndex = (int) (switcher.incrementAndGet() % bufferCount);
        bit = (nextIndex << 2) | NEED_RESET;
        if (sync) {
            for (; ; ) {
                if ((bit << 30) >>> 30 == RESET_DONE)
                    break;
                else
                    Thread.yield();
            }
        }
    }

    @Override
    public Histogram update(long value) {
        count.increment();
        int currBit = bit;
        int currentIndex = currBit >> 2;
        ByteBuffer cur = bufs[currentIndex];
        if ((currBit << 30) >>> 30 == NEED_RESET) {
            bit = currentIndex << 2 | RESET_DONE;
        }
        if (cur.remaining() > 0) {
            cur.putLong(value);
        } else if (type == HistogramType.RANDOM_VITTERS_R) {
            long next;
            for (; ; ) {
                if ((next = nextLong(cur.limit())) <= cur.limit() - BYTES_PER_LONG)
                    break;
            }
            int pos = (int) Math.round((double) next / BYTES_PER_LONG) * BYTES_PER_LONG;
            cur.position(pos);
            cur.putLong(value);
        }
        return this;
    }

    @Override
    public boolean isReady() {
        return bufs[bit >> 2].remaining() == 0;
    }

    @Override
    public void destroy() {
        UnsafeUtils.destroyDirectBuffer(this.bufs[0]);
        UnsafeUtils.destroyDirectBuffer(this.bufs[1]);
    }

    protected int currentIndex() {
        return (int) (switcher.get() % bufferCount);
    }

    protected int prevIndex() {
        if (currentIndex() == 1) return 0;
        else return 1;
    }
}
