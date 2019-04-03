package org.reveno.atp.metrics;

public interface Sink {

    void init();

    void send(String name, String value, long timestamp);

    void close();

    boolean isAvailable();

}
