package org.reveno.atp.api.dynamic;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.reveno.atp.api.commands.CommandContext;
import org.reveno.atp.api.transaction.TransactionContext;
import org.reveno.atp.core.engine.components.CommandsManager;
import org.reveno.atp.core.engine.components.SerializersChain;
import org.reveno.atp.core.engine.components.TransactionsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

@SuppressWarnings("all")
public class DirectTransactionBuilder {
    protected static final Logger log = LoggerFactory.getLogger(DirectTransactionBuilder.class);
    protected static final String PACKAGE = "org.reveno.atp.api.dynamic.";
    protected static final String COMMAND_NAME_PREFIX = "DynamicCommand_";
    protected static final String TRANSACTION_NAME_PREFIX = "DynamicTransaction_";

    protected final String name;
    protected final Set<Class<?>> entityTypes = new HashSet<>();
    protected final BiConsumer<AbstractDynamicTransaction, TransactionContext> transactionHandler;
    protected final SerializersChain serializer;
    protected final TransactionsManager transactionsManager;
    protected final CommandsManager commandsManager;
    protected final ClassLoader classLoader;
    protected Optional<Class<?>> entityReturnIdType = Optional.empty();
    protected Class<? extends AbstractDynamicCommand> dynamicCommand;
    protected Class<? extends AbstractDynamicTransaction> dynamicTransaction;
    protected Optional<BiFunction<AbstractDynamicCommand, CommandContext, Object>> commandInterceptor = Optional.empty();
    protected Optional<BiFunction<AbstractDynamicCommand, CommandContext, Boolean>> commandConditionInterceptor = Optional.empty();

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

    public static Optional<DynamicCommand> loadExistedCommand(String name, ClassLoader classLoader) {
        try {
            Class<? extends AbstractDynamicCommand> c = (Class<? extends AbstractDynamicCommand>) classLoader.loadClass(PACKAGE + COMMAND_NAME_PREFIX + name);
            Class<? extends AbstractDynamicTransaction> t = (Class<? extends AbstractDynamicTransaction>) classLoader.loadClass(PACKAGE + TRANSACTION_NAME_PREFIX + name);
            return Optional.of(new DynamicCommand(c, t));
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage(), e);
            return Optional.empty();
        }
    }

    protected void init() {
        try {
            dynamicCommand = new ByteBuddy()
                    .subclass(AbstractDynamicCommand.class)
                    .name(PACKAGE + COMMAND_NAME_PREFIX + name)
                    .make()
                    .load(classLoader, ClassLoadingStrategy.Default.INJECTION)
                    .getLoaded();
        } catch (Exception e) {
            try {
                dynamicCommand = (Class<? extends AbstractDynamicCommand>) classLoader.loadClass(PACKAGE + COMMAND_NAME_PREFIX + name);
            } catch (ClassNotFoundException ignored) {
            }
        }
        try {
            dynamicTransaction = new ByteBuddy()
                    .subclass(AbstractDynamicTransaction.class)
                    .name(PACKAGE + TRANSACTION_NAME_PREFIX + name)
                    .make()
                    .load(classLoader, ClassLoadingStrategy.Default.INJECTION)
                    .getLoaded();
        } catch (Exception e) {
            try {
                dynamicTransaction = (Class<? extends AbstractDynamicTransaction>) classLoader.loadClass(PACKAGE + TRANSACTION_NAME_PREFIX + name);
            } catch (ClassNotFoundException ignored) {
            }
        }
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

        commandsManager.register(dynamicCommand, Object.class, (a, b) -> {
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

                b.executeTxAction(tx);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return result;
        });
        transactionsManager.registerTransaction((Class<AbstractDynamicTransaction>) dynamicTransaction, transactionHandler);

        return new DynamicCommand(dynamicCommand, dynamicTransaction);
    }

}
