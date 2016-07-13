package org.reveno.atp.utils;

import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

public class RevenoUtils {

    public static boolean waitFor(Supplier<Boolean> condition, long timeoutNanos) {
        long start = System.nanoTime();
        boolean result = false;
        while (System.nanoTime() - start < timeoutNanos) {
            result = condition.get();
            if (result) break;

            LockSupport.parkNanos(1);
        }
        return result;
    }

}
