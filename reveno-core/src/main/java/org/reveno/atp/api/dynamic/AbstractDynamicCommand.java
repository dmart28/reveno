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

import java.util.Map;

public class AbstractDynamicCommand {

	protected Map<String, Object> args;
	public Map<String, Object> args() {
		return args;
	}
	public AbstractDynamicCommand args(Map<String, Object> args) {
		this.args = args;
		return this;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T arg() {
		return (T) args.values().iterator().next();
	}
	
	@SuppressWarnings("unchecked")
	public <T> T arg(String name) {
		return (T) args.get(name);
	}
	
	public AbstractDynamicCommand() {
	}
	
	public AbstractDynamicCommand(Map<String, Object> args) {
		this.args = args;
	}
	
}
