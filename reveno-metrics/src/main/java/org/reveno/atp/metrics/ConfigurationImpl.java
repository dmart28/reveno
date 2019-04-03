package org.reveno.atp.metrics;

import org.reveno.atp.utils.MeasureUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ConfigurationImpl implements Configuration {

    protected static final Logger log = LoggerFactory.getLogger(ConfigurationImpl.class);
    protected boolean sendToGraphite = false;
    protected boolean sendToLog = true;
    protected String graphiteServer;
    protected int graphitePort = 2004;
    protected String instanceName = "default";
    protected String hostName;
    protected int metricBufferSize = MeasureUtils.kb(128);

    public ConfigurationImpl() {
        try {
            this.hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.error(e.getMessage(), e);
        }
    }

    public boolean sendToGraphite() {
        return sendToGraphite;
    }

    @Override
    public Configuration sendToGraphite(boolean sendToGraphite) {
        this.sendToGraphite = sendToGraphite;
        return this;
    }

    public boolean sendToLog() {
        return sendToLog;
    }

    @Override
    public Configuration sendToLog(boolean sendToLog) {
        this.sendToLog = sendToLog;
        return this;
    }

    public String graphiteServer() {
        return graphiteServer;
    }

    @Override
    public Configuration graphiteServer(String graphiteServer) {
        this.graphiteServer = graphiteServer;
        return this;
    }

    public int graphitePort() {
        return graphitePort;
    }

    @Override
    public Configuration graphitePort(int graphitePort) {
        this.graphitePort = graphitePort;
        return this;
    }

    public String instanceName() {
        return instanceName;
    }

    @Override
    public Configuration instanceName(String instanceName) {
        this.instanceName = instanceName;
        return this;
    }

    public String hostName() {
        return hostName;
    }

    @Override
    public Configuration hostName(String hostName) {
        this.hostName = hostName;
        return this;
    }

    public int metricBufferSize() {
        return metricBufferSize;
    }

    @Override
    public Configuration metricBufferSize(int size) {
        this.metricBufferSize = size;
        return this;
    }

}
