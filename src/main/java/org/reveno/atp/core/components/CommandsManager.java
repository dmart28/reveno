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

package org.reveno.atp.core.components;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.reveno.atp.api.commands.CommandContext;

public class CommandsManager {

	public <T, U> void register(Class<T> command, Class<U> result, BiFunction<T, CommandContext, U> handler) {
		commandsHandlers.put(command, new CommandWithResult<T, U>(result, handler));
	}
	
	public <T> void register(Class<T> command, BiConsumer<T, CommandContext> handler) {
		commandsHandlers.put(command, new CommandWithResult<T, Void>(handler));
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
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
	
	protected Map<Class<?>, CommandWithResult<?, ?>> commandsHandlers = new HashMap<>();
	
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
