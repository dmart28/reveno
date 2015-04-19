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

package org.reveno.atp.core;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.reveno.atp.api.ClusterManager;
import org.reveno.atp.api.Configuration;
import org.reveno.atp.api.EventsManager;
import org.reveno.atp.api.Reveno;
import org.reveno.atp.api.RevenoManager;
import org.reveno.atp.api.commands.EmptyResult;
import org.reveno.atp.api.commands.Result;
import org.reveno.atp.api.query.QueryManager;

public class Engine implements Reveno {

	@Override
	public boolean isStarted() {
		return false;
	}

	@Override
	public void startup() {

	}

	@Override
	public void shutdown() {

	}

	@Override
	public RevenoManager domain() {
		return null;
	}

	@Override
	public QueryManager query() {
		return null;
	}

	@Override
	public EventsManager events() {
		return null;
	}

	@Override
	public ClusterManager cluster() {
		return null;
	}

	@Override
	public Configuration config() {
		return null;
	}

	@Override
	public <R> CompletableFuture<Result<R>> executeCommand(Object command) {
		return null;
	}

	@Override
	public CompletableFuture<EmptyResult> performCommands(List<Object> commands) {
		return null;
	}

}
