package org.reveno.atp.utils;

import com.google.common.io.Files;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Date;

public class VersionedFileUtilsTest {

	@Test
	public void test() throws IOException {
		File temp = Files.createTempDir();
		
		String fileName = VersionedFileUtils.nextVersionFile(temp, "tx");
		
		Assert.assertTrue(fileName.startsWith("tx-" + VersionedFileUtils.format().format(new Date()) + "-00000000000000000001"));
		
		new File(temp, fileName).createNewFile();
		
		String nextFile = VersionedFileUtils.nextVersionFile(temp, "tx");
		
		Assert.assertNotEquals(nextFile, fileName);
		Assert.assertTrue(nextFile.compareTo(fileName) == 1);
		
		temp.delete();
	}
	
}
