package org.reveno.atp.metrics;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.reveno.atp.metrics.meter.Histogram;
import org.reveno.atp.metrics.meter.HistogramType;
import org.reveno.atp.metrics.meter.impl.TwoBufferHistogram;
import org.reveno.atp.utils.MeasureUtils;

public class TestMetrics {

	protected final String NAME = "reveno.core";
	
	@Test
	public void testHistogram() {
		MockSink sink = new MockSink();
		Histogram histogram = new TwoBufferHistogram(NAME, MeasureUtils.kb(128));
		histogram.update(13).update(23).update(12).update(44).update(55);
		histogram.sendTo(Collections.singletonList(sink), false);
		
		Assert.assertEquals(sink.getMetrics().get(NAME + ".mean"), "29");
		Assert.assertEquals(sink.getMetrics().get(NAME + ".stddev"), "0");
		Assert.assertEquals(sink.getMetrics().get(NAME + ".min"), "12");
		Assert.assertEquals(sink.getMetrics().get(NAME + ".max"), "55");
		
		histogram.update(14).update(23).update(11).update(47).update(55);
		histogram.sendTo(Collections.singletonList(sink), false);
		
		Assert.assertEquals(sink.getMetrics().get(NAME + ".mean"), "30");
		Assert.assertEquals(sink.getMetrics().get(NAME + ".stddev"), "16");
		Assert.assertEquals(sink.getMetrics().get(NAME + ".min"), "11");
		Assert.assertEquals(sink.getMetrics().get(NAME + ".max"), "55");
		
		histogram.destroy();
		sink.getMetrics().clear();
		
		histogram = new TwoBufferHistogram(NAME, 16);
		histogram.update(13).update(23).update(12).update(44).update(55);
		histogram.sendTo(Collections.singletonList(sink), false);
		
		Assert.assertEquals(sink.getMetrics().get(NAME + ".mean"), "18");
		Assert.assertEquals(sink.getMetrics().get(NAME + ".min"), "13");
		Assert.assertEquals(sink.getMetrics().get(NAME + ".max"), "23");
		
		histogram = new TwoBufferHistogram(NAME, 16, HistogramType.RANDOM_VITTERS_R);
		histogram.update(13).update(23).update(12).update(44).update(55);
		histogram.sendTo(Collections.singletonList(sink), false);
		
		Assert.assertNotEquals(sink.getMetrics().get(NAME + ".mean"), "18");
	}
	
	
	protected static class MockSink implements Sink {
		protected Map<String, String> metrics = new HashMap<>();
		
		public Map<String, String> getMetrics() {
			return metrics;
		}

		@Override
		public void init() {
		}

		@Override
		public void send(String name, String value, long timestamp) {
			metrics.put(name, value);
		}

		@Override
		public void close() {
		}

		@Override
		public boolean isAvailable() {
			return true;
		}
		
	}
	
}
