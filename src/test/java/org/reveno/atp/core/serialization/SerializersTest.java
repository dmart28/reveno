package org.reveno.atp.core.serialization;

import java.util.Arrays;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;
import org.reveno.atp.api.domain.RepositoryData;
import org.reveno.atp.core.api.TransactionCommitInfo;
import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.api.serialization.RepositoryDataSerializer;
import org.reveno.atp.core.api.serialization.TransactionInfoSerializer;
import org.reveno.atp.core.channel.BufferMock;
import org.reveno.atp.core.impl.TransactionCommitInfoImpl;

public class SerializersTest {

	@Test
	public void defaultJavaTest() {
		DefaultJavaSerializer ser = new DefaultJavaSerializer();	
		test(ser, ser);
	}
	
	@Test
	public void protostuffTest() {
		ProtostuffSerializer ser = new ProtostuffSerializer();
		ser.registerTransactionType(User.class);
		
		test(ser, ser);
	}
	
	@Test(expected = RuntimeException.class)
	public void protostuffExceptionTest() {
		ProtostuffSerializer ser = new ProtostuffSerializer();
		ser.registerTransactionType(Empty.class);
		
		test(ser, ser);
	}
	
	public void test(TransactionInfoSerializer tiSer, RepositoryDataSerializer rdSer) {
		final User u1 = new User("Artem", 22);
		final User u2 = new User("Maxim", 28);
		
		RepositoryData data = new RepositoryData(new HashMap<>());
		data.data.put(User.class, new HashMap<>());
		data.data.get(User.class).put(1L, u1);
		data.data.get(User.class).put(2L, u2);
		
		Buffer buffer = new BufferMock();
		rdSer.serialize(data, buffer);
		data = rdSer.deserialize(buffer);
		
		Assert.assertEquals(u1, data.data.get(User.class).get(1L));
		Assert.assertEquals(u2, data.data.get(User.class).get(2L));
		
		buffer.clear();
		
		TransactionCommitInfo ti = new TransactionCommitInfoImpl.PojoBuilder().create()
				.transactionId(2).version(3).time(4).transactionCommits(Arrays.asList(new Object[] { u1, u2 }));
		tiSer.serialize(ti, buffer);
		ti = tiSer.deserialize(new TransactionCommitInfoImpl.PojoBuilder(), buffer);
		
		Assert.assertEquals(ti.transactionId(), 2L);
		Assert.assertEquals(ti.version(), 3L);
		Assert.assertEquals(ti.time(), 4);
		Assert.assertSame(2, ti.transactionCommits().size());
		Assert.assertEquals(u1, ti.transactionCommits().get(0));
		Assert.assertEquals(u2, ti.transactionCommits().get(1));
	}
	
	public static class Empty {}
	
}
