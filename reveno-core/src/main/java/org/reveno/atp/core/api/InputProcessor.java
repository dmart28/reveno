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

package org.reveno.atp.core.api;

import org.reveno.atp.core.api.channel.Buffer;

import java.util.function.Consumer;

/**
 * Previosly we used to use Journaler for both reading and writing.
 * Such logic breaks the idea of Journaler. Its single responsibility is to give 
 * comprehensive (or not) approach for writing.
 * 
 * We could simply put CQRS pattern here as well, segregating read and write parts.
 * InputProcessor will pay purpose of reading from stores.
 * 
 * @author Artem Dmitriev <art.dm.ser@gmail.com>
 *
 */
public interface InputProcessor extends AutoCloseable {

	void process(Consumer<Buffer> consumer, JournalType type);

	
	public enum JournalType {
		TRANSACTIONS, EVENTS
	}
	
}
