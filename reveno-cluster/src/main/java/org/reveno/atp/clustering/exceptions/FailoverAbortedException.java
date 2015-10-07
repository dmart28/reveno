package org.reveno.atp.clustering.exceptions;

public class FailoverAbortedException extends RuntimeException {

    public FailoverAbortedException(String message) {
        super(message);
    }

    public FailoverAbortedException(String message, Throwable t) {
        super(message, t);
    }

}
