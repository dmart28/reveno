package org.reveno.atp.core.engine;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import org.reveno.atp.api.commands.EmptyResult;
import org.reveno.atp.api.commands.Result;
import org.reveno.atp.api.transaction.TransactionInterceptor;
import org.reveno.atp.api.transaction.TransactionStage;
import org.reveno.atp.commons.BoolBiConsumer;
import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.api.channel.Channel;
import org.reveno.atp.core.disruptor.ProcessorContext;
import org.reveno.atp.core.engine.components.TransactionExecutor;
import org.reveno.atp.core.engine.processor.PipeProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

public class InputHandlers {
	private static final EmptyResult EMPTY_RESULT = new EmptyResult();
	protected WorkflowContext services;
	protected TransactionExecutor txExecutor;
	protected LongSupplier nextTransactionId;
	protected LongSupplier transactionId;
	protected ProcessorContext ctxJ;
	protected List<TransactionInterceptor> replicationInterceptors;
	protected List<TransactionInterceptor> transactionInterceptors;
	protected List<TransactionInterceptor> journalingInterceptors;
	private boolean changedClassLoaderReplicator = false;
	private boolean endOfBatch = false;
	private boolean changedClassLoaderJournaler = false;
	private Map<Class<?>, Long2ObjectLinkedOpenHashMap<Object>> markedEntitiesStore;

	protected final Consumer<Buffer> journalerConsumer = b -> {
		ctxJ.commitInfo().transactionId(ctxJ.transactionId()).time(ctxJ.time()).transactionCommits(ctxJ.getTransactions());
		services.serializer().serialize(ctxJ.commitInfo(), b);
	};
	protected final BoolBiConsumer<ProcessorContext> replicator = (c, eob) -> {
		if (!changedClassLoaderReplicator) {
			changeClassLoaderIfRequired();
			changedClassLoaderReplicator = true;
		}
	};
	protected final BoolBiConsumer<ProcessorContext> transactionImmutableExecutor = (c, eob) -> {
		Map<Class<?>, Long2ObjectLinkedOpenHashMap<Object>> old = null;
		if (endOfBatch) {
			markedEntitiesStore = c.getMarkedRecords();
		} else if (markedEntitiesStore != null && !eob) {
			old = c.getMarkedRecords();
			c.setMarkedRecords(markedEntitiesStore);
		}
		try {
			txExecutor.executeCommands(c, services);
		} finally {
			if (old != null) {
				c.setMarkedRecords(old);
				c.skipViews();
			}
			endOfBatch = eob;
		}
	};
	protected final BoolBiConsumer<ProcessorContext> transactionMutableExecutor = (c, eob) -> {
		txExecutor.executeCommands(c, services);
	};

	protected final BoolBiConsumer<ProcessorContext> journaler = (c, eob) -> {
		rollIfRequired(c.transactionId());
		this.ctxJ = c;
		if (!changedClassLoaderJournaler) {
			changeClassLoaderIfRequired();
			changedClassLoaderJournaler = true;
		}
		services.transactionJournaler().writeData(journalerConsumer, eob);
	};
	protected final BoolBiConsumer<ProcessorContext> viewsImmutableUpdater = (c, eob) -> {
		if (!c.isSkipViews()) {
			services.viewsProcessor().process(c.getMarkedRecords());
		}
	};
	protected final BoolBiConsumer<ProcessorContext> viewsMutableUpdater = (c, eob) -> {
		services.viewsProcessor().process(c.getMarkedRecords());
	};
	protected final BoolBiConsumer<ProcessorContext> eventsPublisher = (c, eob) -> {
		if (c.isReplicated()) {
			services.eventPublisher().replicateEvents(c.transactionId());
		} else {
			if (c.getEvents().size() > 0) {
				services.eventPublisher().publishEvents(c.isRestore(), c.transactionId(),
						c.eventMetadata(), c.getEvents().toArray());
			}
		}
	};

	@SuppressWarnings("unchecked")
	public void ex(ProcessorContext c, boolean filter, boolean eob, TransactionStage stage,
				   List<TransactionInterceptor> interceptors, BoolBiConsumer<ProcessorContext> body) {
		ex(c, filter, eob, stage, interceptors, body, false);
	}

	@SuppressWarnings("unchecked")
	public void ex(ProcessorContext c, boolean filter, boolean eob,
				   BoolBiConsumer<ProcessorContext> body) {
		ex(c, filter, eob, null, null, body, false);
	}

	@SuppressWarnings("unchecked")
	public void ex(ProcessorContext c, boolean filter, boolean eob,
				   BoolBiConsumer<ProcessorContext> body, boolean isLast) {
		ex(c, filter, eob, null, null, body, isLast);
	}

	@SuppressWarnings("unchecked")
	public void ex(ProcessorContext c, boolean filter, boolean eob,
				   TransactionStage stage, List<TransactionInterceptor> interceptors,
				   BoolBiConsumer<ProcessorContext> body, boolean isLast) {
		if ((c.isSystem() && !c.isAborted()) || (!c.isAborted() && filter)) {
			try {
				if (c.isSystem()) {
					c.transactionId(transactionId.getAsLong());
				}
				if (stage != null && interceptors != null) {
					interceptors(stage, interceptors, c);
				}
				if (!c.isSystem()) {
					body.accept(c, eob);
				}
			} catch (Throwable t) {
				log.error("inputHandlers", t);
				c.abort(t);
				if (c.future() != null) {
					c.future().complete(new EmptyResult(t));
				}
			}
		} else if (isLast && isSync(c)) {
			c.future().complete(EMPTY_RESULT);
		}
	}

	public void replication(ProcessorContext c, boolean endOfBatch) {
		ex(c, !c.isRestore() && !c.isSync() && !c.isReplicated(), endOfBatch,
				TransactionStage.REPLICATION, replicationInterceptors, replicator);
	}
	
	public void transactionImmutableExecution(ProcessorContext c, boolean endOfBatch) {
		if (!c.isRestore() && !c.isSync()) {
			c.transactionId(nextTransactionId.getAsLong());
		}

		ex(c, !c.isSync(), endOfBatch, TransactionStage.TRANSACTION, transactionInterceptors,
				transactionImmutableExecutor);
	}

	public void transactionMutableExecution(ProcessorContext c, boolean endOfBatch) {
		if (!c.isRestore() && !c.isSync()) {
			c.transactionId(nextTransactionId.getAsLong());
		}

		ex(c, !c.isSync(), endOfBatch, TransactionStage.TRANSACTION, transactionInterceptors,
				transactionMutableExecutor);
	}
	
	public void journaling(ProcessorContext c, boolean endOfBatch) {
		ex(c, !c.isSync() && c.getTransactions().size() > 0 && !c.isRestore(), endOfBatch,
				TransactionStage.JOURNALING, journalingInterceptors, journaler);
	}
	
	public void viewsImmutableUpdate(ProcessorContext c, boolean endOfBatch) {
		ex(c, !c.isSync(), endOfBatch, viewsImmutableUpdater);
	}

	public void viewsMutableUpdate(ProcessorContext c, boolean endOfBatch) {
		ex(c, !c.isSync(), endOfBatch, viewsMutableUpdater);
	}
	
	public void eventsPublishing(ProcessorContext c, boolean endOfBatch) {
		ex(c, c.getEvents().size() > 0, endOfBatch, eventsPublisher, true);
	}
	
	@SuppressWarnings("unchecked")
	public void result(ProcessorContext c, boolean endOfBatch) {
		if (!(c.isRestore() || isSync(c))) {
			if (c.isAborted()) {
				c.future().complete(new EmptyResult(c.abortIssue()));
			}
			else {
				if (c.isHasResult())
					c.future().complete(new Result<>(c.commandResult()));
				else
					c.future().complete(EMPTY_RESULT);
			}
		}
	}
	
	public void destroy() {
	}

	protected void interceptors(TransactionStage stage, List<TransactionInterceptor> interceptors, ProcessorContext c) {
		if (!c.isRestore() && interceptors.size() > 0)
			for (int i = 0; i < interceptors.size(); i++) {
				interceptors.get(i).intercept(c.transactionId(), c.time(), c.systemFlag(), services.repository(), stage);
			}
	}

	protected void rollIfRequired(long transactionId) {
		if (isRollRequired(services.transactionJournaler().currentChannel())) {
			services.journalsManager().roll(transactionId, () -> isRollRequired(services.transactionJournaler().currentChannel()));
		}
	}

	protected boolean isRollRequired(Channel ch) {
		return services.configuration().revenoJournaling().isPreallocated() && ch.size() - ch.position() <= 0;
	}

	protected void changeClassLoaderIfRequired() {
		if (Thread.currentThread().getContextClassLoader() != services.classLoader()) {
			Thread.currentThread().setContextClassLoader(services.classLoader());
		}
	}

	protected boolean isSync(ProcessorContext c) {
		return (c.systemFlag() & PipeProcessor.SYNC_FLAG) == PipeProcessor.SYNC_FLAG;
	}
	
	
	public InputHandlers(WorkflowContext context, LongSupplier nextTransactionId, LongSupplier transactionId) {
		this.services = context;
		this.txExecutor = new TransactionExecutor();
		this.nextTransactionId = nextTransactionId;
		this.transactionId = transactionId;

		replicationInterceptors = context.interceptorCollection().getInterceptors(TransactionStage.REPLICATION);
		transactionInterceptors = context.interceptorCollection().getInterceptors(TransactionStage.TRANSACTION);
		journalingInterceptors = context.interceptorCollection().getInterceptors(TransactionStage.JOURNALING);
	}
	
	protected static final Logger log = LoggerFactory.getLogger(InputHandlers.class);
}
