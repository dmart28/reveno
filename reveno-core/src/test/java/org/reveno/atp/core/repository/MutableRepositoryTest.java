package org.reveno.atp.core.repository;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.reveno.atp.api.domain.WriteableRepository;
import org.reveno.atp.core.serialization.ProtostuffSerializer;

import java.util.HashSet;
import java.util.Set;

public class MutableRepositoryTest {

	private WriteableRepository underlyingRepository;
	private MutableModelRepository repository;
	
	@Before
	public void setUp() {
		underlyingRepository = new HashMapRepository(16, 0.75f);
		repository = new MutableModelRepository(underlyingRepository, new ProtostuffSerializer());
	}
	
	@After
	public void tearDown() {
		repository.destroy();
	}
	
	@Test
	public void testRollback() {
		Bin item1 = new Bin("item1", "value1");
		Bin item2 = new Bin("item2", "value2");
		
		repository.begin();
		repository.store(1L, item1);
		repository.store(2L, item2);
		
		Assert.assertTrue(repository.has(Bin.class, 1L));
		Assert.assertTrue(repository.has(Bin.class, 2L));
		Assert.assertFalse(repository.has(Bin.class, 3L));
		
		Assert.assertTrue(underlyingRepository.has(Bin.class, 1L));
		Assert.assertTrue(underlyingRepository.has(Bin.class, 2L));
		
		repository.rollback();
		
		Assert.assertFalse(underlyingRepository.has(Bin.class, 1L));
		Assert.assertFalse(underlyingRepository.has(Bin.class, 2L));
		Assert.assertFalse(repository.has(Bin.class, 1L));
		Assert.assertFalse(repository.has(Bin.class, 2L));
	}
	
	@Test
	public void testCommit() {
		Bin item1 = new Bin("item1", "value1");
		Bin item2 = new Bin("item2", "value2");
		
		repository.begin();
		repository.store(1L, item1);
		repository.store(2L, item2);
		
		Assert.assertNotNull(repository.get(Bin.class, 1L));
		Assert.assertNotNull(repository.get(Bin.class, 2L));
		Assert.assertNotNull(underlyingRepository.get(Bin.class, 1L));
		Assert.assertNotNull(underlyingRepository.get(Bin.class, 2L));
		
		repository.commit();
		
		Assert.assertNotNull(repository.get(Bin.class, 1L));
		Assert.assertNotNull(repository.get(Bin.class, 2L));
		Assert.assertNotNull(underlyingRepository.get(Bin.class, 1L));
		Assert.assertNotNull(underlyingRepository.get(Bin.class, 2L));
	}
	
	@Test
	public void testTwoEntities() {
		Bin item1 = new Bin("item1", "value1");
		Bin item2 = new Bin("item2", "value2");
		
		repository.begin();
		repository.store(1L, item1);
		repository.store(2L, item2);
		
		Record rec = new Record();
		repository.store(1L, rec);
		Assert.assertTrue(repository.has(Bin.class, 1L));
		Assert.assertTrue(repository.has(Bin.class, 2L));
		Assert.assertTrue(repository.has(Record.class, 1L));
		repository.commit();
		
		Assert.assertTrue(underlyingRepository.has(Bin.class, 1L));
		Assert.assertTrue(underlyingRepository.has(Bin.class, 2L));
		Assert.assertTrue(underlyingRepository.has(Record.class, 1L));
		
		repository.begin();
		rec = repository.get(Record.class, 1L);
		rec.addBin(1L);
		repository.rollback();
		
		Assert.assertEquals(0, repository.get(Record.class, 1L).bins.size());
		
		repository.begin();
		rec = repository.get(Record.class, 1L);
		rec.addBin(1L);
		rec.addBin(2L);
		repository.commit();
		
		Assert.assertEquals(2, repository.get(Record.class, 1L).bins.size());
		Assert.assertArrayEquals(new Long[] {1L, 2L}, repository.get(Record.class, 1L).bins.toArray());
		
		repository.begin();
		rec = repository.get(Record.class, 1L);
		rec.removeBin(1L);
		repository.commit();
		
		Assert.assertEquals(1, repository.get(Record.class, 1L).bins.size());
		Assert.assertArrayEquals(new Long[] {2L}, repository.get(Record.class, 1L).bins.toArray());
		
		rec = repository.get(Record.class, 1L);
		rec.addBin(1L);
		
		repository.begin();
		Assert.assertEquals(2, repository.getEntities(Bin.class).size());
		repository.store(3L, new Bin("artem", "test"));
		Assert.assertEquals(3, repository.getEntities(Bin.class).size());
		repository.rollback();
		Assert.assertEquals(2, repository.getEntities(Bin.class).size());
		
		repository.begin();
		Assert.assertEquals(2, repository.getEntities(Bin.class).size());
		repository.store(3L, new Bin("artem", "test"));
		Assert.assertEquals(3, repository.getEntities(Bin.class).size());
		repository.commit();
		Assert.assertEquals(3, repository.getEntities(Bin.class).size());
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
