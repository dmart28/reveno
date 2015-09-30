package org.reveno.atp.utils;

@SuppressWarnings("all")
public class Exceptions {

    public static RuntimeException runtime(Throwable t) {
        return Exceptions.<RuntimeException>wrap(t);
    }

    private static <T extends Throwable> T wrap(Throwable t) throws T {
        throw (T) t;
    }

}
