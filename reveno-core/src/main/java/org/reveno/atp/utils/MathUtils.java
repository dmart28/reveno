package org.reveno.atp.utils;

public class MathUtils {

    public static int next2n(int size) {
        int i = 1;
        while (true) {
            if (Math.pow(2, ++i) > size)
                break;
        }
        return (int)Math.pow(2, i);
    }

}
