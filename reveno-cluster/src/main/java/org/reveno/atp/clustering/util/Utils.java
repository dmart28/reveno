package org.reveno.atp.clustering.util;

import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

public abstract class Utils {

    public static boolean waitFor(Supplier<Boolean> condition, long timeout) {
        long start = System.nanoTime();
        timeout = timeout * 1_000_000;
        boolean result = false;
        while (System.nanoTime() - start < timeout) {
            result = condition.get();
            if (result) break;

            LockSupport.parkNanos(1);
        }
        return result;
    }

}
