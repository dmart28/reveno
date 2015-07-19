package org.reveno.atp.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.reveno.atp.utils.MeasureUtils.*;

public class MeasureUtilsTest {

	@Test
	public void test() {
		assertEquals(5 * 1024, kb(5));
		assertEquals(2 * 1024 * 1024, mb(2));
		assertEquals(5368709120L, gb(5));
	}
	
}
