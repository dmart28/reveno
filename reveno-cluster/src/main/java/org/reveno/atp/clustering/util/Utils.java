package org.reveno.atp.clustering.util;

import org.reveno.atp.utils.Exceptions;

import java.io.*;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
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

    public static int[] getFreePorts(int portNumber) {
        try {
            int[] result = new int[portNumber];
            List<ServerSocket> servers = new ArrayList<>(portNumber);
            try {
                for (int i = 0; i < portNumber; i++) {
                    ServerSocket tempServer = new ServerSocket(0);
                    servers.add(tempServer);
                    result[i] = tempServer.getLocalPort();
                }
            } finally {
                for (ServerSocket server : servers) {
                    try {
                        server.close();
                    } catch (IOException e) {
                        // Continue closing servers.
                    }
                }
            }
            return result;
        } catch (Throwable t) {
            throw Exceptions.runtime(t);
        }
    }

    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().equals("");
    }

    public static void write(String str, File file) throws FileNotFoundException {
        try (PrintStream out = new PrintStream(new FileOutputStream(file))) {
            out.print(str);
        }
    }

}
