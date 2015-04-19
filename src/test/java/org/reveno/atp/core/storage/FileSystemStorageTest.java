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

package org.reveno.atp.core.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.reveno.atp.core.api.storage.JournalsStorage.JournalStore;
import org.reveno.atp.core.api.storage.SnapshotStorage.SnapshotStore;
import org.reveno.atp.test.utils.FileUtils;

import com.google.common.io.Files;

public class FileSystemStorageTest {

	private File tempDir;
	private FileSystemStorage storage;

	@Before
	public void setUp() {
		tempDir = Files.createTempDir();
		storage = new FileSystemStorage(tempDir);
	}

	@After
	public void tearDown() throws IOException {
		FileUtils.delete(tempDir);
	}

	@Test
	public void genericTest() throws IOException {
		assertEquals(0, storage.getLastStores().length);
		assertEquals(null, storage.getLastSnapshotStore());
		
		JournalStore store1 = storage.nextStore();
		assertNotNull(store1);
		assertEquals("1", store1.getStoreVersion());
		assertNotNull(store1.getEventsCommitsAddress());
		assertNotNull(store1.getTransactionCommitsAddress());
		
		JournalStore[] stores = storage.getLastStores();
		assertEquals(1, stores.length);
		assertEquals(0, stores[0].compareTo(store1));
		assertEquals(stores[0].getEventsCommitsAddress(), store1.getEventsCommitsAddress());
		assertEquals(stores[0].getTransactionCommitsAddress(), store1.getTransactionCommitsAddress());
		
		storage.nextStore();
		stores = storage.getLastStores();
		assertEquals(2, stores.length);
		assertEquals("2", stores[1].getStoreVersion());
		
		SnapshotStore ss = storage.nextSnapshotStore();
		assertNotNull(ss);
		assertTrue(ss.getSnapshotPath().endsWith("-2"));
		
		assertEquals(0, storage.getLastStores().length);
		
		storage.nextStore();
		assertEquals(1, storage.getLastStores().length);
	}
	
	@Test
	public void foldersTest() throws IOException {
		
	}

}
