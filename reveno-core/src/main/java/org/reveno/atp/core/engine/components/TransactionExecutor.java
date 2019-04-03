package org.reveno.atp.core.engine.components;

import org.reveno.atp.api.Configuration.ModelType;
import org.reveno.atp.api.Configuration.MutableModelFailover;
import org.reveno.atp.api.commands.CommandContext;
import org.reveno.atp.api.domain.Repository;
import org.reveno.atp.api.domain.WriteableRepository;
import org.reveno.atp.api.transaction.EventBus;
import org.reveno.atp.api.transaction.TransactionContext;
import org.reveno.atp.core.api.IdGenerator;
import org.reveno.atp.core.disruptor.ProcessorContext;
import org.reveno.atp.core.engine.WorkflowContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.function.Consumer;

public class TransactionExecutor {
    protected static final Logger log = LoggerFactory.getLogger(TransactionExecutor.class);
    protected RecordingRepository repository = new RecordingRepository();
    protected InnerCommandContext commandContext = new InnerCommandContext();
    protected InnerTransactionContext transactionContext = new InnerTransactionContext();

    public void executeCommands(ProcessorContext c, WorkflowContext services) {
        List<Object> cmds = c.getCommands();
        List<Object> txs = c.getTransactions();
        try {
            c.eventBus().currentTransactionId(c.transactionId()).underlyingEventBus(c.defaultEventBus());
            repository.underlying(services.repository()).map(c.getMarkedRecords()).enableReadMark();
            transactionContext.withContext(c).withRepository(repository).reset();
            commandContext.withRepository(repository).withTransactionsHolder(c.getTransactions()).idGenerator(services.idGenerator());

            begin(services);

            if (c.isRestore()) {
                executeTransactions(services, txs, 0);
            } else {
                int index = 0;
                for (int i = 0; i < cmds.size(); i++) {
                    repository.disableReadMark();
                    Object result = services.commandsManager().execute(cmds.get(i), commandContext);
                    repository.enableReadMark();
                    executeTransactions(services, txs, index);
                    if (c.isHasResult())
                        c.commandResult(result);
                    index = txs.size();
                }
            }

            commit(services);
        } catch (Throwable t) {
            c.abort(t);
            log.error("executeCommands", t);
            rollback(services, c);
        }
    }

    protected void rollback(WorkflowContext services, ProcessorContext c) {
        if (services.configuration().modelType() == ModelType.MUTABLE &&
                services.configuration().mutableModelFailover() == MutableModelFailover.SNAPSHOTS) {
            services.repository().rollback();
        } else {
            compensateTransactions(services, c.getTransactions().listIterator(c.getTransactions().size() - 1));
        }
    }

    protected void commit(WorkflowContext services) {
        if (services.configuration().modelType() == ModelType.MUTABLE &&
                services.configuration().mutableModelFailover() == MutableModelFailover.SNAPSHOTS)
            services.repository().commit();
    }

    protected void begin(WorkflowContext services) {
        if (services.configuration().modelType() == ModelType.MUTABLE &&
                services.configuration().mutableModelFailover() == MutableModelFailover.SNAPSHOTS)
            services.repository().begin();
    }

    protected void executeTransactions(WorkflowContext services, List<Object> txs, int index) {
        for (int i = index; i < txs.size(); i++)
            services.transactionsManager().execute(txs.get(i), transactionContext);
    }

    protected void compensateTransactions(WorkflowContext services, ListIterator<Object> transactionIterator) {
        forEachPrev(transactionIterator, t -> services.transactionsManager().compensate(t, transactionContext));
    }

    protected void forEachPrev(ListIterator<Object> i, Consumer<Object> c) {
        while (i.hasPrevious()) {
            c.accept(i.previous());
        }
    }

    protected static class InnerCommandContext implements CommandContext {
        public Repository repository;
        public IdGenerator idGenerator;
        protected List<Object> transactions;

        @Override
        public Repository repo() {
            return repository;
        }

        @Override
        public long id(Class<?> type) {
            return idGenerator.next(type);
        }

        @Override
        public CommandContext executeTxAction(Object transactionAction) {
            transactions.add(transactionAction);
            return this;
        }

        @Override
        public CommandContext executeTransaction(Object transactionAction) {
            return executeTxAction(transactionAction);
        }

        public InnerCommandContext withRepository(Repository repository) {
            this.repository = repository;
            return this;
        }

        public InnerCommandContext withTransactionsHolder(List<Object> transactions) {
            this.transactions = transactions;
            return this;
        }

        public InnerCommandContext idGenerator(IdGenerator idGenerator) {
            this.idGenerator = idGenerator;
            this.idGenerator.context(this);
            return this;
        }
    }

    ;

    protected static class InnerTransactionContext implements TransactionContext {
        public EventBus eventBus;
        public WriteableRepository repository;
        // TODO make much more efficient
        protected Map<Object, Object> map = new HashMap<>();

        @Override
        public EventBus eventBus() {
            return eventBus;
        }

        @Override
        public WriteableRepository repo() {
            return repository;
        }

        public InnerTransactionContext withContext(ProcessorContext context) {
            this.eventBus = context.eventBus();
            return this;
        }

        public InnerTransactionContext withRepository(WriteableRepository repository) {
            this.repository = repository;
            return this;
        }

        public void reset() {
            map.clear();
        }

        @Override
        public Map<Object, Object> data() {
            return map;
        }
    }

}
