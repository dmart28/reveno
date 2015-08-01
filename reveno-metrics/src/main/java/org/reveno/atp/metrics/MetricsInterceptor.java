package org.reveno.atp.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.reveno.atp.api.domain.WriteableRepository;
import org.reveno.atp.api.transaction.TransactionInterceptor;
import org.reveno.atp.api.transaction.TransactionStage;
import org.reveno.atp.metrics.impl.GraphiteSink;
import org.reveno.atp.metrics.impl.Slf4jSink;
import org.reveno.atp.metrics.meter.Counter;
import org.reveno.atp.metrics.meter.Histogram;
import org.reveno.atp.metrics.meter.impl.SimpleCounter;
import org.reveno.atp.metrics.meter.impl.TwoBufferHistogram;

public class MetricsInterceptor implements TransactionInterceptor {
	
	protected long start = -1L;

	@Override
	public void intercept(long transactionId, long time, WriteableRepository repository, TransactionStage stage) {
		if (stage == TransactionStage.REPLICATION) {
			counter.inc();
			histogram.update(System.nanoTime() - time);
		}
	}

	public MetricsInterceptor(ConfigurationImpl config) {
		String prefix = "reveno.instances." + config.hostName().replace(".", "_") + "." + config.instanceName() + ".";
		this.latency = prefix + "latency";
		this.throughput = prefix + "throughput";
		this.config = config;
		this.counter = new SimpleCounter(throughput);
		this.histogram = new TwoBufferHistogram(latency, config.metricBufferSize());
	}
	
	public void init() {
		if (config.sendToGraphite()) {
			sinks.add(new GraphiteSink(config.graphiteServer(), config.graphitePort()));
		}
		if (config.sendToLog()) {
			sinks.add(new Slf4jSink());
		}
		sinks.forEach(Sink::init);
		executor.scheduleAtFixedRate(() -> {
			histogram.sendTo(sinks, true);
			counter.sendTo(sinks, true);
		}, 15, 15, TimeUnit.SECONDS);
	}
	
	public void shutdown() {
		sinks.forEach(Sink::close);
		if (histogram != null) {
			histogram.destroy();
		}
		executor.shutdown();
	}
	
	protected Counter counter;
	protected Histogram histogram;
	protected List<Sink> sinks = new ArrayList<>();
	protected final String latency, throughput;
	protected final ConfigurationImpl config;
	protected final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
}
