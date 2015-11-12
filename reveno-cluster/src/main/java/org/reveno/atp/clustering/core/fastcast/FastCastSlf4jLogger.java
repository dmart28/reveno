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

package org.reveno.atp.clustering.core.fastcast;

import org.nustaq.fastcast.util.FCLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FastCastSlf4jLogger extends FCLog {

    @Override
    public void info(Throwable e) {
        LOG.info(e.getMessage(), e);
    }

    @Override
    public void info(String msg) {
        LOG.info(msg);
    }

    @Override
    public void info(String msg, Throwable e) {
        LOG.info(msg, e);
    }

    @Override
    public void warn(Throwable e) {
        LOG.warn(e.getMessage(), e);
    }

    @Override
    public void warn(String msg) {
        LOG.warn(msg);
    }

    @Override
    public void warn(String msg, Throwable e) {
        LOG.warn(msg, e);
    }

    @Override
    public void severe(String msg, Throwable e) {
        LOG.error(msg, e);
    }

    @Override
    public void fatal(String msg) {
        LOG.error(msg);
    }

    @Override
    public void fatal(String msg, Throwable e) {
        LOG.error(msg, e);
    }

    @Override
    public void debug(Throwable e) {
        LOG.debug(e.getMessage(), e);
    }

    @Override
    public void debug(String msg) {
        LOG.debug(msg);
    }

    @Override
    public void debug(String msg, Throwable e) {
        LOG.debug(msg, e);
    }

    @Override
    public void net(String s) {
        LOG.info(s);
    }

    protected static final Logger LOG = LoggerFactory.getLogger(FastCastSlf4jLogger.class);
}
