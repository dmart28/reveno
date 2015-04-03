package org.reveno.atp.utils;

import static org.junit.Assert.*;
import static org.reveno.atp.utils.MeasureUtils.*;

import org.junit.Test;

public class MeasureUtilsTest {

	@Test
	public void test() {
		assertEquals(5 * 1024, kb(5));
		assertEquals(2 * 1024 * 1024, mb(2));
		assertEquals(5368709120L, gb(5));
	}
	
}
