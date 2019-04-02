package org.reveno.atp.core.engine.components;

import org.reveno.atp.api.transaction.TransactionContext;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class TransactionsManager {
	protected Map<Class<?>, BiConsumer<Object, TransactionContext>> txs = new HashMap<>();
	protected Map<Class<?>, BiConsumer<Object, TransactionContext>> compensateTxs = new HashMap<>();

	public <T> void registerTransaction(Class<T> transactionType, BiConsumer<T, TransactionContext> handler) {
		registerTransaction(transactionType, handler, false);
	}
	
	@SuppressWarnings("unchecked")
	public <T> void registerTransaction(Class<T> transactionType, BiConsumer<T, TransactionContext> handler, boolean isCompensate) {
		if (!isCompensate) {
			txs.put(transactionType, (BiConsumer<Object, TransactionContext>) handler);
		} else {
			compensateTxs.put(transactionType, (BiConsumer<Object, TransactionContext>) handler);
		}
	}
	
	public void execute(Object transaction, TransactionContext context) {
		txs.get(transaction.getClass()).accept(transaction, context);
	}
	
	public void compensate(Object transaction, TransactionContext context) {
		BiConsumer<Object, TransactionContext> compensate = compensateTxs.get(transaction.getClass());
		if (compensate != null)
			compensate.accept(transaction, context);
	}
	
}
