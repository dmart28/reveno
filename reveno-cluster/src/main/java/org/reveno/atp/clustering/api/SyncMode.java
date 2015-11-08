package org.reveno.atp.clustering.api;

import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectOpenHashMap;

public enum SyncMode {
    SNAPSHOT((byte) 1), JOURNALS((byte) 2);

    protected byte type;
    protected static final Byte2ObjectMap<SyncMode> map = new Byte2ObjectOpenHashMap<>();

    public byte getType() {
        return type;
    }

    public static SyncMode get(byte type) {
        return map.get(type);
    }

    static {
        for (SyncMode mode : values()) {
            map.put(mode.getType(), mode);
        }
    }

    SyncMode(byte type) {
        this.type = type;
    }
}
