package org.reveno.atp.metrics;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import org.reveno.atp.api.domain.WriteableRepository;
import org.reveno.atp.api.transaction.TransactionInterceptor;
import org.reveno.atp.api.transaction.TransactionStage;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;

public class MetricsInterceptor implements TransactionInterceptor {
	
	protected long start = -1L;

	@Override
	public void intercept(long transactionId, long time, WriteableRepository repository, TransactionStage stage) {
		if (stage == TransactionStage.REPLICATION) {
			metrics.meter(throughput).mark();
			metrics.histogram(latency).update(System.nanoTime() - time);
		}
	}

	public MetricsInterceptor(ConfigurationImpl config) {
		String prefix = "reveno.instances." + config.hostName().replace(".", "_") + "." + config.instanceName() + ".";
		this.latency = prefix + "latency";
		this.throughput = prefix + "throughput";
		this.config = config;
	}
	
	public void init() {
		if (config.sendToGraphite()) {
			graphite = new Graphite(new InetSocketAddress(config.graphiteServer(), config.graphitePort()));
			reporter = GraphiteReporter.forRegistry(metrics)
					.convertRatesTo(TimeUnit.SECONDS)
                    .convertDurationsTo(TimeUnit.MILLISECONDS)
					.filter(MetricFilter.ALL)
					.build(graphite);
			reporter.start(1, TimeUnit.MINUTES);
		}
		if (config.sendToLog()) {
			slf4jReporter = Slf4jReporter.forRegistry(metrics)
                    .outputTo(LoggerFactory.getLogger(MetricsInterceptor.class))
                    .convertRatesTo(TimeUnit.SECONDS)
                    .convertDurationsTo(TimeUnit.MILLISECONDS)
                    .build();
			slf4jReporter.start(1, TimeUnit.MINUTES);
		}
	}
	
	public void shutdown() {
		if (reporter != null) {
			reporter.stop();
		}
		if (slf4jReporter != null) {
			slf4jReporter.stop();
		}
	}
	
	protected final MetricRegistry metrics = new MetricRegistry();
	protected final String latency, throughput;
	protected final ConfigurationImpl config;
	protected Graphite graphite;
	protected GraphiteReporter reporter;
	protected Slf4jReporter slf4jReporter;
}
