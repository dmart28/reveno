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
