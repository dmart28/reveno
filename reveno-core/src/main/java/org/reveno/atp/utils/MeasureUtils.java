package org.reveno.atp.utils;

public abstract class MeasureUtils {

    public static int kb(int number) {
        return 1024 * number;
    }

    public static int mb(int number) {
        return 1024 * kb(number);
    }

    public static long gb(int number) {
        return 1024L * mb(number);
    }

    public static long sec(int secs) {
        return secs * 1000 * 1_000_000L;
    }

    public static long sec(double secs) {
        return (long) (secs * 1000 * 1_000_000L);
    }

}
