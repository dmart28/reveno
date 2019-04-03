package org.reveno.atp.core.api;

import java.io.Serializable;

public class SystemInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    public long lastTransactionId;

    public SystemInfo(long lastTransactionId) {
        this.lastTransactionId = lastTransactionId;
    }

}
