package org.reveno.atp.core.channel;

import com.google.common.io.Files;
import org.junit.Assert;
import org.junit.Test;
import org.reveno.atp.api.ChannelOptions;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.reveno.atp.utils.MeasureUtils.mb;

public class FileChannelTest {

	@Test
	public void testRead() throws IOException {
		final byte[] testData = new byte[mb(3) + 13];
		new Random().nextBytes(testData);
		File testFile = new File(Files.createTempDir(), "fileChannelTest.dat");
		Files.write(testData, testFile);
		
		FileChannel fc = new FileChannel(testFile, ChannelOptions.UNBUFFERED_IO);
        ByteBufferWrapper buf = new ByteBufferWrapper(java.nio.ByteBuffer.allocate(1024 * 1024));
		
		try {
            while (fc.isReadAvailable())
                fc.read(buf);

            Assert.assertEquals(mb(3) + 13, buf.readerPosition());
            buf.getBuffer().flip();
            Assert.assertArrayEquals(testData, buf.getBytes());

            Assert.assertTrue(fc.isOpen());
            fc.close();
            Assert.assertFalse(fc.isOpen());
        } finally {
			testFile.delete();
		}
	}
	
	@Test
	public void testWrite() throws IOException {
		final int chunks = 13;
		List<byte[]> dataSets = IntStream.range(0, chunks).mapToObj((i) -> new byte[mb(1)])
			.peek((i) -> new Random().nextBytes(i)).collect(Collectors.toList());
		
		File testFile = new File(Files.createTempDir(), "fileChannelTest.dat");
		try {
			FileChannel fc = new FileChannel(testFile, ChannelOptions.UNBUFFERED_IO);
			dataSets.forEach((ds) -> fc.write(b -> b.writeBytes(ds), true));
			fc.close();
		
			Assert.assertEquals(mb(chunks) + (chunks * 4), testFile.length());
			Assert.assertFalse(fc.isOpen());
		} finally {
			testFile.delete();
		}
	}
	
}
