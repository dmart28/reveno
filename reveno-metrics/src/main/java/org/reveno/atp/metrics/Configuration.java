package org.reveno.atp.metrics;

public interface Configuration {

	Configuration sendToGraphite(boolean sendToGraphite);

	Configuration sendToLog(boolean sendToLog);

	Configuration graphiteServer(String graphiteServer);

	Configuration graphitePort(int graphitePort);

	Configuration instanceName(String instanceName);

	Configuration hostName(String hostName);
	
	Configuration metricBufferSize(int size);

}