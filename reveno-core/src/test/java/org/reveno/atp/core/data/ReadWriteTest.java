package org.reveno.atp.core.data;

import com.google.common.io.Files;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.reveno.atp.core.RevenoConfiguration;
import org.reveno.atp.core.api.InputProcessor;
import org.reveno.atp.core.api.InputProcessor.JournalType;
import org.reveno.atp.core.api.Journaler;
import org.reveno.atp.core.api.TransactionCommitInfo;
import org.reveno.atp.core.api.channel.Channel;
import org.reveno.atp.core.api.serialization.TransactionInfoSerializer;
import org.reveno.atp.core.api.storage.JournalsStorage.JournalStore;
import org.reveno.atp.core.engine.components.SerializersChain;
import org.reveno.atp.core.impl.TransactionCommitInfoImpl.PojoBuilder;
import org.reveno.atp.core.serialization.ProtostuffSerializer;
import org.reveno.atp.core.storage.FileSystemStorage;
import org.reveno.atp.test.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ReadWriteTest {
	
	private File tempDir;
	private FileSystemStorage storage;

	@Before
	public void setUp() {
		tempDir = Files.createTempDir();
		storage = new FileSystemStorage(tempDir, new RevenoConfiguration.RevenoJournalingConfiguration());
	}

	@After
	public void tearDown() throws IOException {
		FileUtils.delete(tempDir);
	}
	
	@Test
	public void test() throws Exception {
		List<TransactionInfoSerializer> serializers = new ArrayList<>();
		serializers.add(new ProtostuffSerializer());
		SerializersChain serializer = new SerializersChain(serializers);
		serializer.registerTransactionType(User.class);
		PojoBuilder builder = new PojoBuilder();
		// there will be 10 journal stores
		final int totalCount = 10 * 100_000;
		int count = 0;
		for (int i = 1; i <= 10; i++) {
			JournalStore store = storage.nextStore();
			Channel channel = storage.channel(store.getTransactionCommitsAddress());
			Journaler journaler = new DefaultJournaler();
			journaler.startWriting(channel);
			
			for (int j = 1; j <= totalCount / 10; j++) {
				User user = new User(Double.toString(Math.random()));
				TransactionCommitInfo d = builder.create()
						.transactionId(System.currentTimeMillis()).time(count++).transactionCommits(
								Arrays.asList(new Object[] { user }));
				journaler.writeData(b -> serializer.serialize(d, b), Math.random() < 0.1);
			}
			journaler.writeData(b -> b.writeBytes(new byte[0]), true);
			journaler.stopWriting();
			channel.close();
		}

		storage.nextStore();
		CountDownLatch l = new CountDownLatch(totalCount);
		InputProcessor processor = new DefaultInputProcessor(storage);
		processor.process(0, b -> {
			try {
				while (b.isAvailable()) {
					Assert.assertEquals(totalCount - l.getCount(), serializer.deserialize(builder, b).time());
					l.countDown();
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}, JournalType.TRANSACTIONS);
		Assert.assertTrue(l.await(30, TimeUnit.SECONDS));
		processor.close();
	}
	
	public static class User {
		public String id;
		
		public User(String id) {
			this.id = id;
		}
	}
	
}
