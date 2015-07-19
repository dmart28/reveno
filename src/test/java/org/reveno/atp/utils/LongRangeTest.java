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

package org.reveno.atp.utils;

import org.junit.Assert;
import org.junit.Test;
import org.reveno.atp.commons.LongRange;

import java.util.Arrays;
import java.util.Comparator;
import java.util.TreeSet;

public class LongRangeTest {

	@Test
	public void test() {
		LongRange r1 = new LongRange(5, 8);
		LongRange r2 = new LongRange(11, 13);
		
		LongRange[] ranges = new LongRange[] { r1, r2};
		Arrays.sort(ranges, new Comparator<LongRange>() {
			@Override
			public int compare(LongRange o1, LongRange o2) {
				return o1.compareTo(o2);
			}
		});
		Assert.assertEquals("[[5;8], [11;13]]", Arrays.asList(ranges).toString());
		
		TreeSet<LongRange> ts = new TreeSet<>();
		ts.add(r2);
		ts.add(r1);
		
		Assert.assertEquals(r1, ts.iterator().next());
		
		LongRange[] splitted = r1.split(7);
		
		Assert.assertEquals(2, splitted.length);
		Assert.assertEquals(new LongRange(5, 6), splitted[0]);
		Assert.assertEquals(new LongRange(8), splitted[1]);
		
		splitted = r2.split(12);
		Assert.assertEquals(new LongRange(11), splitted[0]);
		Assert.assertEquals(new LongRange(13), splitted[1]);
		
		splitted = r2.split(13);
		Assert.assertEquals(1, splitted.length);
		Assert.assertEquals(new LongRange(11, 12), splitted[0]);
		
		splitted = r1.split(5);
		Assert.assertEquals(1, splitted.length);
		Assert.assertEquals(new LongRange(6, 8), splitted[0]);
		
		splitted = new LongRange(11, 12).split(11);
		Assert.assertEquals(new LongRange(12), splitted[0]);
		
		splitted = new LongRange(15).split(15);
		Assert.assertEquals(0, splitted.length);
	}
	
}
