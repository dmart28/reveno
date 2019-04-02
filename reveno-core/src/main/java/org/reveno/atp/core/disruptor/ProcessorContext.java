package org.reveno.atp.core.disruptor;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import org.reveno.atp.api.EventsManager.EventMetadata;
import org.reveno.atp.api.transaction.EventBus;
import org.reveno.atp.core.api.Destroyable;
import org.reveno.atp.core.api.RestoreableEventBus;
import org.reveno.atp.core.api.TransactionCommitInfo;
import org.reveno.atp.utils.MapUtils;
import sun.misc.Contended;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("rawtypes")
public class ProcessorContext implements Destroyable {
	@Contended
	private long time = 0L;
	@Contended
	private boolean skipViews = false;
	private boolean isSystem = false;
	@Contended
	private long systemFlag = 0L;
	@Contended
	private long transactionId;
	@Contended
	private CompletableFuture future;
	@Contended
	private boolean isHasResult;
	@Contended
	private Object commandResult;
	@Contended
	private boolean isAborted;
	@Contended
	private final List<Object> transactions = new ArrayList<>();
	@Contended
	private boolean isRestore;
	@Contended
	private boolean isSync;
	@Contended
	private final List<Object> commands = new ArrayList<>();
	@Contended
	private final List<Object> events = new ArrayList<>();
	@Contended
	private EventMetadata eventMetadata;
	@Contended
	private boolean isReplicated;
	@Contended
	private final TransactionCommitInfo commitInfo;
	private Throwable abortIssue;
	private final RestoreableEventBus defaultEventBus = new ProcessContextEventBus();
	private RestoreableEventBus eventBus = defaultEventBus;
	private Map<Class<?>, Long2ObjectLinkedOpenHashMap<Object>> markedRecords = MapUtils.linkedFastRepo();

	public ProcessorContext(TransactionCommitInfo commitInfo) {
		this.commitInfo = commitInfo;
	}

	public long time() {
		return time;
	}

	public boolean isSkipViews() {
		return skipViews;
	}

	public boolean isSystem() {
		return isSystem;
	}

	public long systemFlag() {
		return systemFlag;
	}

	public EventBus defaultEventBus() {
		return defaultEventBus;
	}

	public long transactionId() {
		return transactionId;
	}

	public CompletableFuture future() {
		return future;
	}

	public boolean isHasResult() {
		return isHasResult;
	}

	public boolean isAborted() {
		return isAborted;
	}

	public Throwable abortIssue() {
		return abortIssue;
	}

	public void commandResult(Object commandResult) {
		this.commandResult = commandResult;
	}

	public boolean isRestore() {
		return isRestore;
	}

	public boolean isSync() {
		return isSync;
	}

	public boolean isReplicated() {
		return isReplicated;
	}

	public List<Object> getCommands() {
		return commands;
	}

	public List<Object> getTransactions() {
		return transactions;
	}

	public List<Object> getEvents() {
		return events;
	}

	public Object commandResult() {
		return commandResult;
	}

	public RestoreableEventBus eventBus() {
		return eventBus;
	}

	public EventMetadata eventMetadata() {
		return eventMetadata;
	}

	public TransactionCommitInfo commitInfo() {
		return commitInfo;
	}

	public Map<Class<?>, Long2ObjectLinkedOpenHashMap<Object>> getMarkedRecords() {
		return markedRecords;
	}

	public ProcessorContext time(long time) {
		this.time = time;
		return this;
	}

	public ProcessorContext skipViews() {
		this.skipViews = true;
		return this;
	}

	public ProcessorContext replicated() {
		isReplicated = true;
		return this;
	}

	public ProcessorContext systemFlag(long systemFlag) {
		this.isSystem = true;
		this.systemFlag = systemFlag;
		return this;
	}

	public ProcessorContext transactionId(long transactionId) {
		this.transactionId = transactionId;
		return this;
	}

	public ProcessorContext future(CompletableFuture future) {
		this.future = future;
		return this;
	}

	public ProcessorContext withResult() {
		this.isHasResult = true;
		return this;
	}

	public void abort(Throwable abortIssue) {
		this.isAborted = true;
		this.abortIssue = abortIssue;
	}

	public ProcessorContext restore() {
		isRestore = true;
		return this;
	}

	public ProcessorContext sync() {
		this.isSync = true;
		return this;
	}

	public ProcessorContext addCommand(Object cmd) {
		commands.add(cmd);
		return this;
	}

	public ProcessorContext addCommands(List<Object> cmds) {
		commands.addAll(cmds);
		return this;
	}

	public ProcessorContext addTransactions(List<Object> transactions) {
		this.transactions.addAll(transactions);
		return this;
	}

	public ProcessorContext eventBus(RestoreableEventBus eventBus) {
		this.eventBus = eventBus;
		return this;
	}

	public ProcessorContext eventMetadata(EventMetadata eventMetadata) {
		this.eventMetadata = eventMetadata;
		return this;
	}

	public ProcessorContext setMarkedRecords(Map<Class<?>, Long2ObjectLinkedOpenHashMap<Object>> markedRecords) {
		this.markedRecords = markedRecords;
		return this;
	}
	
	public ProcessorContext reset() {
		transactionId = 0L;
		systemFlag = 0L;
		isSystem = false;
		isReplicated = false;
		skipViews = false;
		commands.clear();
		transactions.clear();
		events.clear();
		markedRecords.values().forEach(Long2ObjectLinkedOpenHashMap::clear);
		isHasResult = false;
		isAborted = false;
		isSync = false;
		isRestore = false;
		abortIssue = null;
		future = null;
		commandResult = null;
		eventMetadata = null;
		eventBus = defaultEventBus;
		
		return this;
	}
	
	public void destroy() {
		reset();
	}
	
	protected class ProcessContextEventBus implements RestoreableEventBus {
		@Override
		public void publishEvent(Object event) {
			ProcessorContext.this.getEvents().add(event);
		}

		@Override
		public RestoreableEventBus currentTransactionId(long transactionId) {
			return this;
		}

		@Override
		public RestoreableEventBus underlyingEventBus(EventBus eventBus) {
			return this;
		}
	}
	
}
