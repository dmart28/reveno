package org.reveno.atp.core.engine.processor;

import org.reveno.atp.api.commands.EmptyResult;
import org.reveno.atp.api.commands.Result;
import org.reveno.atp.core.api.Destroyable;
import org.reveno.atp.core.api.RestoreableEventBus;
import org.reveno.atp.core.api.TransactionCommitInfo;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface TransactionPipeProcessor<T extends Destroyable> extends PipeProcessor<T> {

    CompletableFuture<EmptyResult> process(List<Object> commands);

    <R> CompletableFuture<Result<R>> execute(Object command);

    void executeRestore(RestoreableEventBus eventBus, TransactionCommitInfo transaction);

}
