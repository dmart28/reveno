package org.reveno.atp.core.api;

import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectOpenHashMap;
import org.reveno.atp.api.transaction.TransactionInterceptor;
import org.reveno.atp.api.transaction.TransactionStage;

import java.util.ArrayList;
import java.util.List;

public class InterceptorCollection {
    private Byte2ObjectMap<List<TransactionInterceptor>> interceptors = new Byte2ObjectOpenHashMap<>();

    public List<TransactionInterceptor> getInterceptors(TransactionStage stage) {
        return interceptors.computeIfAbsent(stage.getType(), k -> new ArrayList<>());
    }

    public void add(TransactionStage stage, TransactionInterceptor interceptor) {
        getInterceptors(stage).add(interceptor);
    }

}
