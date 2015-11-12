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
