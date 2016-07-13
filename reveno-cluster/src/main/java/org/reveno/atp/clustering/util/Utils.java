/**
 *  Copyright (c) 2015 The original author or authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */


package org.reveno.atp.clustering.util;

import org.reveno.atp.utils.Exceptions;

import java.io.*;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

public abstract class Utils {

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
