package org.reveno.atp.utils;

@SuppressWarnings("all")
public class Exceptions {

    public static void runtime(Throwable t) {
        Exceptions.<RuntimeException>wrap(t);
    }

    private static <T extends Throwable> T wrap(Throwable t) throws T {
        throw (T) t;
    }

}
