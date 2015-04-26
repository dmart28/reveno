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

package org.reveno.atp.core.repository;

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.reveno.atp.api.domain.WriteableRepository;

public class RepositoryTest {
	
	private WriteableRepository repository;
	
	@Before
	public void setUp() {
		repository = new HashMapRepository();
	}
	
	@Test
	public void testBasic() {
		Bin item1 = new Bin("item1", "value1");
		Bin item2 = new Bin("item2", "value2");
		
		repository.store(1L, item1);
		repository.store(2L, item2);
		
		Assert.assertTrue(repository.get(Bin.class, 1L).isPresent());
		Assert.assertTrue(repository.get(Bin.class, 2L).isPresent());
		Assert.assertFalse(repository.get(Bin.class, 3L).isPresent());
		
		Assert.assertTrue(repository.get(Bin.class, 1L).isPresent());
		Assert.assertTrue(repository.get(Bin.class, 2L).isPresent());
		
		repository.remove(Bin.class, 2L);
		Assert.assertEquals(1, repository.getAll(Bin.class).size());
		
		Record rec = new Record();
		repository.store(1L, rec);
		repository.store(2L, item2);
		
		Assert.assertEquals(2, repository.getAll().size());
		Assert.assertEquals(2, repository.getAll(Bin.class).size());
		Assert.assertEquals(1, repository.getAll(Record.class).size());
	}
	
	public static class Record {
		private final Set<Long> bins;
		
		public void addBin(long bin) {
			bins.add(bin);
		}
		
		public void removeBin(long bin) {
			bins.remove(bin);
		}
		
		public Record() {
			this.bins = new HashSet<>();
		}
	}
	
	public static class Bin {
		private String name;
		public String getName() {
			return name;
		}
		
		private Object value;
		public Object getValue() {
			return value;
		}
		
		public Bin(String name, Object value) {
			this.name = name;
			this.value = value;
		}
	}
	
}
