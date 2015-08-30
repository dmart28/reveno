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

package org.reveno.atp.metrics.impl;

import org.reveno.atp.metrics.Sink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class GraphiteSink implements Sink {

	@Override
	public void init() {
		try {
			graphite.connect();
		} catch (IllegalStateException | IOException e) {
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public void send(String name, String value, long timestamp) {
		try {
			graphite.send(name, value, timestamp);
		} catch (IOException e) {
			log.error("", e);
		}
	}

	@Override
	public void close() {
		try {
			graphite.close();
		} catch (IOException e) {
			log.error("", e);
		}
	}

	@Override
	public boolean isAvailable() {
		return graphite.isConnected();
	}

	public GraphiteSink(String hostname, int port) {
		graphite = new PickledGraphite(hostname, port);
	}
	
	protected final PickledGraphite graphite;
	protected static final Logger log = LoggerFactory.getLogger(GraphiteSink.class);
	
}
