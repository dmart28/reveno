package org.reveno.atp.api.commands.dynamic;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.reveno.atp.api.commands.CommandContext;
import org.reveno.atp.api.transaction.TransactionContext;
import org.reveno.atp.core.engine.components.CommandsManager;
import org.reveno.atp.core.engine.components.SerializersChain;
import org.reveno.atp.core.engine.components.TransactionsManager;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;

public class DirectTransactionBuilder {
	
	protected static final String PACKAGE = "org.reveno.atp.api.commands.dynamic.";
	protected static final String COMMAND_NAME_PREFIX = "DynamicCommand_";
	protected static final String TRANSACTION_NAME_PREFIX = "DynamicTransaction_";

	public DirectTransactionBuilder(String name, BiConsumer<AbstractTransaction, TransactionContext> handler,
			SerializersChain serializer, TransactionsManager transactionsManager, CommandsManager commandsManager,
			ClassLoader classLoader) {
		this.name = name;
		this.transactionHandler = handler;
		this.serializer = serializer;
		this.transactionsManager = transactionsManager;
		this.commandsManager = commandsManager;
		this.classLoader = classLoader;
	}

	protected void init() {
		dynamicCommand = new ByteBuddy()
				  .subclass(AbstractCommand.class)
				  .name(PACKAGE + COMMAND_NAME_PREFIX + name)
				  .make()
				  .load(classLoader, ClassLoadingStrategy.Default.INJECTION)
				  .getLoaded();
		dynamicTransaction = new ByteBuddy()
				  .subclass(AbstractTransaction.class)
				  .name(PACKAGE + TRANSACTION_NAME_PREFIX + name)
				  .make()
				  .load(classLoader, ClassLoadingStrategy.Default.INJECTION)
				  .getLoaded();
		serializer.registerTransactionType(dynamicCommand);
		serializer.registerTransactionType(dynamicTransaction);
	}
	
	public DirectTransactionBuilder uniqueIdFor(Class<?>... entityTypes) {
		this.entityTypes.addAll(Arrays.asList(entityTypes));
		this.entityReturnIdType = Optional.of(entityTypes[0]);
		return this;
	}
	
	public DirectTransactionBuilder returnsIdOf(Class<?> entityReturnIdType) {
		this.entityReturnIdType = Optional.of(entityReturnIdType);
		return this;
	}
	
	public DirectTransactionBuilder commandInterceptor(BiFunction<AbstractCommand, CommandContext, Object> commandInterceptor) {
		this.commandInterceptor = Optional.of(commandInterceptor);
		return this;
	}
	
	@SuppressWarnings("unchecked")
	public DynamicCommand command() {
		init();
		
		commandsManager.register((Class<AbstractCommand>) dynamicCommand, Object.class, (a, b) -> {
			Object result = null;
			if (commandInterceptor.isPresent()) {
				result = commandInterceptor.get().apply(a, b);
			}
			try {
				AbstractTransaction tx = dynamicTransaction.newInstance();
				tx.args.putAll(a.args);
				entityTypes.forEach(c -> tx.ids.put(c, b.id(c)));

				if (result == null && entityReturnIdType.isPresent()) {
					result = tx.ids.get(entityReturnIdType.get());
				}
				
				b.executeTransaction(tx);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			return result;
		});
		transactionsManager.registerTransaction((Class<AbstractTransaction>) dynamicTransaction, transactionHandler);
		
		return new DynamicCommand(dynamicCommand, dynamicTransaction);
	}
	
	protected String name;
	protected Set<Class<?>> entityTypes = new HashSet<>();
	protected Optional<Class<?>> entityReturnIdType = Optional.empty();
	protected Class<? extends AbstractCommand> dynamicCommand;
	protected Class<? extends AbstractTransaction> dynamicTransaction;
	protected Optional<BiFunction<AbstractCommand, CommandContext, Object>> commandInterceptor = Optional.empty();
	
	protected BiConsumer<AbstractTransaction, TransactionContext> transactionHandler;
	protected SerializersChain serializer;
	protected TransactionsManager transactionsManager;
	protected CommandsManager commandsManager;
	protected ClassLoader classLoader;
	
}
