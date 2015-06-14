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

package org.reveno.atp.api;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.reveno.atp.api.commands.CommandContext;
import org.reveno.atp.api.query.ViewsMapper;
import org.reveno.atp.api.transaction.TransactionContext;
import org.reveno.atp.core.api.serialization.TransactionInfoSerializer;

public interface RevenoManager {

	<T> void transactionAction(Class<T> transaction, BiConsumer<T, TransactionContext> handler);
	
	<C, U> void command(Class<C> commandType, Class<U> resultType, BiFunction<C, CommandContext, U> handler);
	
	<C> void command(Class<C> commandType, BiConsumer<C, CommandContext> handler);
	
	<E, V> void viewMapper(Class<E> entityType, Class<V> viewType, ViewsMapper<E, V> mapper);
	
	RevenoManager snapshotWith(RepositorySnapshotter snapshotter);
	
	void restoreWith(RepositorySnapshotter snapshotter);
	
	void andRestoreWithIt();
	
	void resetSnapshotters();
	
	void serializeWith(List<TransactionInfoSerializer> serializers);
		
	
	default RevenoManager and(RepositorySnapshotter snapshotter) {
		Objects.requireNonNull(snapshotter);
		
		snapshotWith(snapshotter);
		return this;
	}
	
}
