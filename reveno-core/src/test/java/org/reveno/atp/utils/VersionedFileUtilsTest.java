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
