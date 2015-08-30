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

package org.reveno.atp.core.api.serialization;

import org.reveno.atp.core.api.TransactionCommitInfo;
import org.reveno.atp.core.api.channel.Buffer;

import java.util.List;

public interface TransactionInfoSerializer extends Serializer {
	
	void serialize(TransactionCommitInfo info, Buffer buffer);
	
	TransactionCommitInfo deserialize(TransactionCommitInfo.Builder builder, Buffer buffer);
	
	void serializeCommands(List<Object> commands, Buffer buffer);
	
	List<Object> deserializeCommands(Buffer buffer);
	
}
