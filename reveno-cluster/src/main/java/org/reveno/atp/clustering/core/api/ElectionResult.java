package org.reveno.atp.clustering.core.api;

public class ElectionResult {
    public final boolean isMaster;
    public final boolean failed;

    public ElectionResult(boolean master, boolean failed) {
        this.isMaster = master;
        this.failed = failed;
    }
}
