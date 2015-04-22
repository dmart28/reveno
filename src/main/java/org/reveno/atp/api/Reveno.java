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
import java.util.concurrent.CompletableFuture;

import org.reveno.atp.api.commands.EmptyResult;
import org.reveno.atp.api.commands.Result;
import org.reveno.atp.api.query.QueryManager;

/*
 * Core engine interface.
 */
public interface Reveno {
	
	boolean isStarted();
	
	void startup();
	
	void shutdown();
	
	
	RevenoManager domain();
	
	QueryManager query();
	
	EventsManager events();
	
	ClusterManager cluster();
	
	Configuration config();
	
	
	<R> CompletableFuture<Result<? extends R>> executeCommand(Object command);
	
	CompletableFuture<EmptyResult> performCommands(List<Object> commands);
	
}
