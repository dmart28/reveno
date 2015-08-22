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

import org.reveno.atp.api.ChannelOptions;
import org.reveno.atp.core.api.channel.Channel;
import org.reveno.atp.core.api.storage.FoldersStorage;
import org.reveno.atp.core.api.storage.JournalsStorage;
import org.reveno.atp.core.api.storage.SnapshotStorage;
import org.reveno.atp.core.channel.FileChannel;
import org.reveno.atp.utils.UnsafeUtils;
import org.reveno.atp.utils.VersionedFileUtils;
import org.reveno.atp.utils.VersionedFileUtils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.reveno.atp.utils.VersionedFileUtils.*;

public class FileSystemStorage implements FoldersStorage, JournalsStorage,
		SnapshotStorage {

	@Override
	public Channel channel(String address) {
		return new FileChannel(new File(baseDir, address), ChannelOptions.BUFFERING_VM);
	}

	@Override
	public Channel channel(String address, ChannelOptions options) {
		return new FileChannel(new File(baseDir, address), options);
	}

    @Override
	public SnapshotStore getLastSnapshotStore() {
		VersionedFile file = lastVersionedFile(baseDir, SNAPSHOT_PREFIX);
		if (file != null) {
			return new SnapshotStore(file.getName(), file.getFileDate()
					.getTimeInMillis());
		} else
			return null;
	}

	@Override
	public SnapshotStore nextSnapshotStore() {
		VersionedFile tx = lastVersionedFile(baseDir, TRANSACTION_PREFIX);
		String nextFile = nextVersionFile(baseDir, SNAPSHOT_PREFIX, 
				tx == null ? "1" : Long.toString(tx.getVersion()));
		try {
			new File(baseDir, nextFile).createNewFile();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return new SnapshotStore(nextFile, System.currentTimeMillis());
	}

	@Override
	public void removeLastSnapshotStore() {
		Optional<String> fileName = lastVersionFile(baseDir, SNAPSHOT_PREFIX);
		if (fileName.isPresent())
			new File(baseDir, fileName.get()).delete();
	}

	@Override
	public JournalStore[] getLastStores() {
		final VersionedFile snap = lastVersionedFile(baseDir, SNAPSHOT_PREFIX);
		final List<VersionedFile> txs = snap != null ? afterLastSnapshot(snap, txs()) : txs();
		final List<VersionedFile> evns = snap != null ? afterLastSnapshot(snap, evns()) : evns();
		if (txs.size() != evns.size())
			throw new RuntimeException(String.format("Amount of Transaction files doesn't match to Events files [%s/%s]",
					txs.size(), evns.size()));

		return getJournalStores(txs, evns);
	}

	@Override
	public JournalStore[] getVolumes() {
		final List<VersionedFile> txs = listVersioned(baseDir, VOLUME_EVENTS_PREFIX);
		final List<VersionedFile> evns = listVersioned(baseDir, VOLUME_TRANSACTION_PREFIX);

		if (txs.size() != evns.size())
			return new JournalStore[0];

		return getJournalStores(txs, evns);
	}

	@Override
	public void mergeStores() {
		// TODO implement
	}

	@Override
	public void deleteOldStores() {
		// TODO implement
	}

	@Override
	public JournalStore nextStore() {
		VersionedFile txFile = parseVersionedFile(nextVersionFile(baseDir, TRANSACTION_PREFIX));
		VersionedFile evnFile = parseVersionedFile(nextVersionFile(baseDir, EVENTS_PREFIX));

		if (txFile.getVersion() != evnFile.getVersion())
			throw new RuntimeException(String.format(
					"Versions of Journals are not equal [tx=%d,evn=%d]",
					txFile.getVersion(), evnFile.getVersion()));

		return store(txFile, evnFile);
	}

	@Override
	public JournalStore nextVolume(long txSize, long eventsSize) {
		VersionedFile txFile = parseVersionedFile(nextVersionFile(baseDir, VOLUME_TRANSACTION_PREFIX));
		VersionedFile evnFile = parseVersionedFile(nextVersionFile(baseDir, VOLUME_EVENTS_PREFIX));

		if (txFile.getVersion() < evnFile.getVersion()) {
			IntStream.range(0, (int) Math.abs(txFile.getVersion() - evnFile.getVersion())).forEach(i -> {
				VersionedFile file = parseVersionedFile(nextVersionFile(baseDir, VOLUME_TRANSACTION_PREFIX));
				preallocateFiles(new File(baseDir, file.getName()), txSize);
			});
			txFile = parseVersionedFile(nextVersionFile(baseDir, VOLUME_TRANSACTION_PREFIX));
		} else if (txFile.getVersion() > evnFile.getVersion()) {
			IntStream.range(0, (int) Math.abs(txFile.getVersion() - evnFile.getVersion())).forEach(i -> {
				VersionedFile file = parseVersionedFile(nextVersionFile(baseDir, VOLUME_EVENTS_PREFIX));
				preallocateFiles(new File(baseDir, file.getName()), eventsSize);
			});
			evnFile = parseVersionedFile(nextVersionFile(baseDir, VOLUME_EVENTS_PREFIX));
		}

		preallocateFiles(new File(baseDir, txFile.getName()), txSize);
		preallocateFiles(new File(baseDir, evnFile.getName()), eventsSize);

		return new JournalStore(txFile.getName(), evnFile.getName(), Long.toString(txFile.getVersion()), true);
	}

	@Override
	public JournalStore convertVolumeToStore(JournalStore volume) {
		VersionedFile txFile = parseVersionedFile(nextVersionFile(baseDir, TRANSACTION_PREFIX));
		VersionedFile evnFile = parseVersionedFile(nextVersionFile(baseDir, EVENTS_PREFIX));

		new File(baseDir, volume.getTransactionCommitsAddress()).renameTo(new File(baseDir, txFile.getName()));
		new File(baseDir, volume.getEventsCommitsAddress()).renameTo(new File(baseDir, evnFile.getName()));

		return new JournalStore(txFile.getName(), evnFile.getName(), Long.toString(txFile.getVersion()), false);
	}

	@Override
	public Folder nextFolder(String prefix) {
		String folder = nextVersionFile(baseDir, prefix);
		new File(baseDir, folder).mkdir();
		return new Folder(folder, prefix);
	}

	@Override
	public Folder getLastFolder(String prefix) {
		List<String> folders = listFolders(baseDir, prefix);
		return folders.size() == 0 ? null : new Folder(folders.get(folders.size() - 1), prefix);
	}

	@Override
	public FolderItem[] getItems(Folder folder) {
        List<FolderItem> collect = listFiles(new File(baseDir, folder.getGroupAddress()).toPath()).stream()
                .map(Path::toFile)
                .map(f -> new FolderItem(f.getName(), folder.getGroupAddress() + "/ " + f.getName()))
                .collect(Collectors.toList());
        return collect.toArray(new FolderItem[collect.size()]);
	}

	@Override
	public FolderItem getItem(String name, Folder folder) {
		String address = folder.getGroupAddress() + "/" + name;
		if (new File(baseDir, address).exists())
			return new FolderItem(name, address);
		else return null;
	}

	@Override
	public FolderItem createItem(Folder folder, String name) {
		String address = folder.getGroupAddress() + "/" + name;
		new File(baseDir, address).mkdir();
		return new FolderItem(name, address);
	}

	protected JournalStore[] getJournalStores(List<VersionedFile> txs, List<VersionedFile> evns) {
		List<JournalStore> collect = txs.stream().map(tx -> evns.stream()
				.filter(e -> e.getVersion() == tx.getVersion())
				.map(e -> store(tx, e)).findFirst().get()).collect(Collectors.toList());
		return collect.toArray(new JournalStore[collect.size()]);
	}
	
	protected List<Path> listFiles(Path dir) {
	       List<Path> result = new ArrayList<>();
	       try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*")) {
	           for (Path entry: stream) {
	        	   if (!Files.isDirectory(entry))
	        		   result.add(entry);
	           }
	       } catch (DirectoryIteratorException | IOException ex) {
	           throw new RuntimeException(ex);
	       }
	       return result;
	}
	
	protected List<VersionedFile> evns() {
		return listVersioned(baseDir, EVENTS_PREFIX);
	}

	protected List<VersionedFile> txs() {
		return listVersioned(baseDir, TRANSACTION_PREFIX);
	}
	
	protected List<VersionedFile> afterLastSnapshot(
			VersionedFile lastSnap, List<VersionedFile> txs) {
		return txs.stream().filter(f -> f.getVersion() > lastSnap.getVersion()).collect(Collectors.toList());
	}
	
	protected JournalStore store(VersionedFile txFile, VersionedFile evnFile) {
		try {
			new File(baseDir, txFile.getName()).createNewFile();
			new File(baseDir, evnFile.getName()).createNewFile();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return new JournalStore(txFile.getName(), evnFile.getName(), Long.toString(txFile.getVersion()), false);
	}

	protected void preallocateFiles(File file, long size) {
		try {
			RandomAccessFile raf = new RandomAccessFile(file, "rw");
			LOG.info("Preallocating started.");
			for (long i = 0; i < size; i += PAGE_SIZE) {
				raf.write(BLANK_PAGE, 0, PAGE_SIZE);
			}
			LOG.info("Preallocating finished.");
			raf.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public FileSystemStorage(File baseDir) {
		this.baseDir = baseDir;
	}

	protected final File baseDir;
	protected static final String TRANSACTION_PREFIX = "tx";
	protected static final String SNAPSHOT_PREFIX = "snp";
	protected static final String EVENTS_PREFIX = "evn";
	protected static final String VOLUME_TRANSACTION_PREFIX = "v_" + TRANSACTION_PREFIX;
	protected static final String VOLUME_EVENTS_PREFIX = "v_" + EVENTS_PREFIX;

	protected static final int PAGE_SIZE = UnsafeUtils.getUnsafe().pageSize();
	protected static final byte[] BLANK_PAGE = new byte[PAGE_SIZE];

	protected static final Logger LOG = LoggerFactory.getLogger(FileSystemStorage.class);
}
