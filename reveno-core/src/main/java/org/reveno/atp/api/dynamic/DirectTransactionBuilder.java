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

package org.reveno.atp.api.dynamic;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.reveno.atp.api.commands.CommandContext;
import org.reveno.atp.api.transaction.TransactionContext;
import org.reveno.atp.core.engine.components.CommandsManager;
import org.reveno.atp.core.engine.components.SerializersChain;
import org.reveno.atp.core.engine.components.TransactionsManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class DirectTransactionBuilder {
	
	protected static final String PACKAGE = "org.reveno.atp.api.dynamic.";
	protected static final String COMMAND_NAME_PREFIX = "DynamicCommand_";
	protected static final String TRANSACTION_NAME_PREFIX = "DynamicTransaction_";

	public DirectTransactionBuilder(String name, BiConsumer<AbstractDynamicTransaction, TransactionContext> handler,
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
				  .subclass(AbstractDynamicCommand.class)
				  .name(PACKAGE + COMMAND_NAME_PREFIX + name)
				  .make()
				  .load(classLoader, ClassLoadingStrategy.Default.INJECTION)
				  .getLoaded();
		dynamicTransaction = new ByteBuddy()
				  .subclass(AbstractDynamicTransaction.class)
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
	
	public DirectTransactionBuilder command(BiFunction<AbstractDynamicCommand, CommandContext, Object> commandInterceptor) {
		this.commandInterceptor = Optional.of(commandInterceptor);
		return this;
	}
	
	public DirectTransactionBuilder conditionalCommand(BiFunction<AbstractDynamicCommand, CommandContext, Boolean> commandConditionInterceptor) {
		this.commandConditionInterceptor = Optional.of(commandConditionInterceptor);
		return this;
	}
	
	@SuppressWarnings("unchecked")
	public DynamicCommand command() {
		init();
		
		commandsManager.register((Class<AbstractDynamicCommand>) dynamicCommand, Object.class, (a, b) -> {
			Object result = null;
			if (commandInterceptor.isPresent()) {
				result = commandInterceptor.get().apply(a, b);
			} else if (commandConditionInterceptor.isPresent()) {
				if (!commandConditionInterceptor.get().apply(a, b)) {
					return null;
				}
			}
			try {
				AbstractDynamicTransaction tx = dynamicTransaction.newInstance();
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
		transactionsManager.registerTransaction((Class<AbstractDynamicTransaction>) dynamicTransaction, transactionHandler);
		
		return new DynamicCommand(dynamicCommand, dynamicTransaction);
	}
	
	protected String name;
	protected Set<Class<?>> entityTypes = new HashSet<>();
	protected Optional<Class<?>> entityReturnIdType = Optional.empty();
	protected Class<? extends AbstractDynamicCommand> dynamicCommand;
	protected Class<? extends AbstractDynamicTransaction> dynamicTransaction;
	protected Optional<BiFunction<AbstractDynamicCommand, CommandContext, Object>> commandInterceptor = Optional.empty();
	protected Optional<BiFunction<AbstractDynamicCommand, CommandContext, Boolean>> commandConditionInterceptor = Optional.empty();
	
	protected BiConsumer<AbstractDynamicTransaction, TransactionContext> transactionHandler;
	protected SerializersChain serializer;
	protected TransactionsManager transactionsManager;
	protected CommandsManager commandsManager;
	protected ClassLoader classLoader;
	
}
