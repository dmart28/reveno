package org.reveno.atp.core.engine.components;

import org.reveno.atp.api.commands.CommandContext;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class CommandsManager {

    protected Map<Class<?>, CommandWithResult<?, ?>> commandsHandlers = new HashMap<>();

    public <T, U> void register(Class<T> command, Class<U> result, BiFunction<T, CommandContext, U> handler) {
        commandsHandlers.put(command, new CommandWithResult<T, U>(result, handler));
    }

    public <T> void register(Class<T> command, BiConsumer<T, CommandContext> handler) {
        commandsHandlers.put(command, new CommandWithResult<T, Void>(handler));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public Object execute(Object command, CommandContext context) {
        CommandWithResult cmd = commandsHandlers.get(command.getClass());
        if (cmd == null)
            throw new RuntimeException(String.format("Can't find handler for command [type:%s, cmd:%s]",
                    command.getClass(), command));

        if (cmd.handler != null)
            return cmd.handler.apply(command, context);
        else {
            cmd.emptyHandler.accept(command, context);
            return null;
        }
    }

    protected static class CommandWithResult<T, U> {
        public final BiFunction<T, CommandContext, U> handler;
        public final BiConsumer<T, CommandContext> emptyHandler;
        public final Class<?> resultType;

        public CommandWithResult(Class<U> result, BiFunction<T, CommandContext, U> handler) {
            this.handler = handler;
            this.resultType = result;
            this.emptyHandler = null;
        }

        public CommandWithResult(BiConsumer<T, CommandContext> emptyHandler) {
            this.handler = null;
            this.resultType = null;
            this.emptyHandler = emptyHandler;
        }
    }

}
