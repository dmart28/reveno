package org.reveno.atp.api.domain;

import java.io.Serializable;
import java.util.Map;

public class RepositoryData implements Serializable {
    private static final long serialVersionUID = 5311358734940295761L;
    private final Map<Class<?>, Map<Long, Object>> data;

    public RepositoryData(Map<Class<?>, Map<Long, Object>> data) {
        this.data = data;
    }

    public Map<Class<?>, Map<Long, Object>> getData() {
        return data;
    }
}
