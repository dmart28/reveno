package org.reveno.atp.core.repository;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.reveno.atp.api.domain.WriteableRepository;

import java.util.HashSet;
import java.util.Set;

public class RepositoryTest {
	
	private WriteableRepository repository;
	
	@Before
	public void setUp() {
		repository = new HashMapRepository(16, 0.75f);
	}
	
	@Test
	public void testBasic() {
		Bin item1 = new Bin("item1", "value1");
		Bin item2 = new Bin("item2", "value2");
		
		repository.store(1L, item1);
		repository.store(2L, item2);
		
		Assert.assertTrue(repository.has(Bin.class, 1L));
		Assert.assertTrue(repository.has(Bin.class, 2L));
		Assert.assertFalse(repository.has(Bin.class, 3L));
		
		Assert.assertTrue(repository.has(Bin.class, 1L));
		Assert.assertTrue(repository.has(Bin.class, 2L));
		
		repository.remove(Bin.class, 2L);
		Assert.assertEquals(1, repository.getEntities(Bin.class).size());
		
		Record rec = new Record();
		repository.store(1L, rec);
		repository.store(2L, item2);
		
		Assert.assertEquals(2, repository.getData().getData().size());
		Assert.assertEquals(2, repository.getEntities(Bin.class).size());
		Assert.assertEquals(1, repository.getEntities(Record.class).size());
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
