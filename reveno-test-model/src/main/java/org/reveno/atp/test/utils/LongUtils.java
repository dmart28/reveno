package org.reveno.atp.test.utils;

import java.util.Base64;

public abstract class LongUtils {

    public static String longToBase64(long number) {
        return Base64.getEncoder().encodeToString(longToBytes(number));
    }

    public static byte[] longToBytes(long l) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte) (l & 0xFF);
            l >>= 8;
        }
        return result;
    }

}
