package org.reveno.atp.core.benchmarks;

import org.openjdk.jmh.annotations.*;
import org.reveno.atp.api.Configuration;
import org.reveno.atp.core.EngineWorkflowContext;
import org.reveno.atp.core.RevenoConfiguration;
import org.reveno.atp.core.UnclusteredFailoverManager;
import org.reveno.atp.core.api.InterceptorCollection;
import org.reveno.atp.core.disruptor.DisruptorTransactionPipeProcessor;
import org.reveno.atp.core.disruptor.ProcessorContext;
import org.reveno.atp.core.engine.WorkflowContext;
import org.reveno.atp.core.engine.WorkflowEngine;
import org.reveno.atp.core.engine.components.CommandsManager;
import org.reveno.atp.core.engine.components.DefaultIdGenerator;
import org.reveno.atp.core.engine.components.SerializersChain;
import org.reveno.atp.core.engine.components.TransactionsManager;
import org.reveno.atp.core.engine.processor.PipeProcessor;
import org.reveno.atp.core.engine.processor.TransactionPipeProcessor;
import org.reveno.atp.core.impl.TransactionCommitInfoImpl;
import org.reveno.atp.core.repository.HashMapRepository;
import org.reveno.atp.core.repository.SnapshotBasedModelRepository;
import org.reveno.atp.core.views.ViewsDefaultStorage;
import org.reveno.atp.core.views.ViewsManager;
import org.reveno.atp.core.views.ViewsProcessor;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 15, time = 1)
@Measurement(iterations = 10, time = 1)
public class ViewsMappingBenchmark {

    private BenchmarkWorkflowEngine workflowEngine;
    private IncrementCounter cmd;

    @Setup
    public void init() throws Exception {
        EngineWorkflowContext context = new EngineWorkflowContext();
        // transaction actions
        TransactionsManager transactionsManager = new TransactionsManager();
        transactionsManager.registerTransaction(IncrementCounter.class, (t, ctx) -> {
            ctx.repo().remap(t.id, Counter.class, (id, o) -> o.increment());
        });
        // commands
        CommandsManager commandsManager = new CommandsManager();
        commandsManager.register(IncrementCounter.class, (c, ctx) -> ctx.executeTxAction(c));
        // views
        ViewsManager viewsManager = new ViewsManager();
        viewsManager.register(Counter.class, Long.class, (i, e, r) -> e.count);
        context.classLoader(getClass().getClassLoader())
                .configuration(new RevenoConfiguration())
                .idGenerator(new DefaultIdGenerator())
                .repository(new SnapshotBasedModelRepository(new HashMapRepository(16, 0.75f)))
                .serializers(new SerializersChain(getClass().getClassLoader()))
                .transactionsManager(transactionsManager).commandsManager(commandsManager)
                .viewsProcessor(new ViewsProcessor(viewsManager, new ViewsDefaultStorage(16, 0.75f)))
                .transactionCommitBuilder(new TransactionCommitInfoImpl.PojoBuilder())
                .failoverManager(new UnclusteredFailoverManager())
                .interceptorCollection(new InterceptorCollection());
        workflowEngine = new BenchmarkWorkflowEngine(
                new DisruptorTransactionPipeProcessor(
                        context.transactionCommitBuilder(),
                        Configuration.CpuConsumption.HIGH,
                        512,
                        Executors.newFixedThreadPool(3)),
                context, Configuration.ModelType.IMMUTABLE);
        context.repository().store(1, new Counter(1));
        cmd = new IncrementCounter(1, 1);
        workflowEngine.init();
    }

    @TearDown
    public void tearDown() throws Exception {
        workflowEngine.shutdown();
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void benchmark() throws Exception {
        workflowEngine.getPipe().execute(cmd);
    }

    protected static class BenchmarkWorkflowEngine extends WorkflowEngine {

        public BenchmarkWorkflowEngine(TransactionPipeProcessor<ProcessorContext> inputProcessor,
                                       WorkflowContext context, Configuration.ModelType modelType) {
            super(inputProcessor, context, modelType);
        }

        @Override
        protected void buildPipe(PipeProcessor<ProcessorContext> pipe) {
            pipe.then(handlers::transactionImmutableExecution).then(handlers::viewsImmutableUpdate);
        }
    }

    public static class Counter {
        public final long count;

        public Counter(long count) {
            this.count = count;
        }

        public Counter increment() {
            return new Counter(count + 1);
        }
    }

    public static class IncrementCounter {
        public final long id;
        public final long amount;

        public IncrementCounter(long id, long amount) {
            this.amount = amount;
            this.id = id;
        }
    }

    public static void main(String[] args) throws Exception {
        ViewsMappingBenchmark b = new ViewsMappingBenchmark();
        b.init();
        b.benchmark();
        b.tearDown();
    }

}
