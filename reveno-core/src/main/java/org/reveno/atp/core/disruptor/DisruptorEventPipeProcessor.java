package org.reveno.atp.core.disruptor;

import com.lmax.disruptor.EventFactory;
import org.reveno.atp.api.Configuration.CpuConsumption;
import org.reveno.atp.core.events.Event;
import org.reveno.atp.core.events.EventPublisher;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadFactory;

public class DisruptorEventPipeProcessor extends DisruptorPipeProcessor<Event> {
	protected static final EventFactory<Event> eventFactory = Event::new;
	protected final CpuConsumption cpuConsumption;
	protected final int bufferSize;
	protected final ThreadFactory threadFactory;

	public DisruptorEventPipeProcessor(CpuConsumption cpuConsumption, int bufferSize, ThreadFactory threadFactory) {
		this.cpuConsumption = cpuConsumption;
		this.bufferSize = bufferSize;
		this.threadFactory = threadFactory;
	}

	@Override
	public void sync() {
		CompletableFuture<?> res = process((e,f) -> e.reset().flag(EventPublisher.SYNC_FLAG).syncFuture(f));
		try {
			res.get();
		} catch (Throwable t) {
			log.error("sync", t);
		}
	}

	@Override
	public CpuConsumption cpuConsumption() {
		return cpuConsumption;
	}
	
	@Override
	public int bufferSize() {
		return bufferSize;
	}

	@Override
	boolean singleProducer() {
		return false;
	}

	@Override
	public EventFactory<Event> eventFactory() {
		return eventFactory;
	}

	@Override
	public ThreadFactory threadFactory() {
		return threadFactory;
	}

	@Override
	void startupInterceptor() {
	}
	
}
