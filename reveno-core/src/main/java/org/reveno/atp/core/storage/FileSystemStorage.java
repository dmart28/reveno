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

import org.reveno.atp.core.RevenoConfiguration.RevenoJournalingConfiguration;
import org.reveno.atp.core.api.channel.Channel;
import org.reveno.atp.core.api.storage.FoldersStorage;
import org.reveno.atp.core.api.storage.JournalsStorage;
import org.reveno.atp.core.api.storage.SnapshotStorage;
import org.reveno.atp.core.channel.FileChannel;
import org.reveno.atp.utils.Exceptions;
import org.reveno.atp.utils.UnsafeUtils;
import org.reveno.atp.utils.VersionedFileUtils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.reveno.atp.utils.VersionedFileUtils.*;
import static org.reveno.atp.utils.VersionedFileUtils.parseVersionedFile;

public class FileSystemStorage implements FoldersStorage, JournalsStorage, SnapshotStorage {

	@Override
	public Channel channel(String address) {
		FileChannel fc = new FileChannel(new File(baseDir, address));
		fc.extendDelta(config.maxObjectSize());
		fc.channelOptions(config.channelOptions());
		fc.isPreallocated(config.isPreallocated());
		return fc.init();
	}

	@Override
	public JournalStore[] getAllStores() {
		return getJournalStores(txs(), evns());
	}

	@Override
	public Channel snapshotChannel(String address) {
		return new FileChannel(new File(baseDir, address)).init();
	}

	@Override
	public SnapshotStore getLastSnapshotStore() {
		VersionedFile file = lastVersionedFile(baseDir, SNAPSHOT_PREFIX);
		if (file != null) {
			return new SnapshotStore(file.getName(), file.getFileDate().getTimeInMillis(),
					file.getVersion(), file.getRest().length == 0 ? file.getVersion() : Long.parseLong(file.getRest()[0]));
		} else
			return null;
	}

	@Override
	public JournalStore[] getStoresAfterVersion(long journalVersion) {
		final List<VersionedFile> txs = afterLastSnapshot(journalVersion, txs());
		final List<VersionedFile> evns = afterLastSnapshot(journalVersion, evns());
		if (txs.size() != evns.size())
			throw new RuntimeException(String.format("Amount of Transaction files doesn't match to Events files [%s/%s]",
					txs.size(), evns.size()));

		return getJournalStores(txs, evns);
	}

	@Override
	public SnapshotStore nextSnapshotAfter(long journalVersion) {
		return nextSnapshotAfter("", journalVersion);
	}

	@Override
	public SnapshotStore nextTempSnapshotStore(long journalVersion) {
		return nextSnapshotAfter("temp_", journalVersion);
	}

	public SnapshotStore nextSnapshotAfter(String prefix, long journalVersion) {
		String nextFile = nextVersionFile(baseDir, prefix + SNAPSHOT_PREFIX,
				null, Long.toString(journalVersion));
		try {
			new File(baseDir, nextFile).createNewFile();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		VersionedFile vf = parseVersionedFile(nextFile);
		return new SnapshotStore(nextFile, System.currentTimeMillis(), vf.getVersion(), journalVersion);
	}

	@Override
	public void removeSnapshotStore(SnapshotStore snapshot) {
		new File(baseDir, snapshot.getSnapshotPath()).delete();
	}

	@Override
	public void move(SnapshotStore from, SnapshotStore to) {
		new File(baseDir, from.getSnapshotPath()).renameTo(new File(baseDir, to.getSnapshotPath()));
	}

	@Override
	public void removeLastSnapshotStore() {
		Optional<String> fileName = lastVersionFile(baseDir, SNAPSHOT_PREFIX);
		if (fileName.isPresent())
			new File(baseDir, fileName.get()).delete();
	}

	@Override
	public JournalStore[] getVolumes() {
		List<VersionedFile> txs = listVersioned(baseDir, VOLUME_TRANSACTION_PREFIX);
		List<VersionedFile> evns = listVersioned(baseDir, VOLUME_EVENTS_PREFIX);

		if (txs.size() != evns.size()) {
			txs = txs.subList(0, Math.max(Math.min(txs.size(), evns.size()), 0));
			evns = evns.subList(0, Math.max(Math.min(txs.size(), evns.size()), 0));
		}

		return getJournalStores(txs, evns);
	}

	@Override
	public void mergeStores(JournalStore[] stores, JournalStore to) {
		merge(Arrays.stream(stores).map(JournalStore::getEventsCommitsAddress).collect(Collectors.toList()),
				to.getEventsCommitsAddress());
		merge(Arrays.stream(stores).map(JournalStore::getTransactionCommitsAddress).collect(Collectors.toList()),
				to.getTransactionCommitsAddress());
	}

	@Override
	public void deleteStore(JournalStore store) {
		new File(store.getEventsCommitsAddress()).delete();
		new File(store.getTransactionCommitsAddress()).delete();
	}

	@Override
	public JournalStore nextTempStore() {
		VersionedFile txFile = parseVersionedFile(nextVersionFile(baseDir, TRANSACTION_PREFIX, 0));
		VersionedFile evnFile = parseVersionedFile(nextVersionFile(baseDir, EVENTS_PREFIX, 0));

		return store(txFile, evnFile, "tmp_");
	}

	@Override
	public synchronized JournalStore nextStore() {
		return nextStore(0);
	}

	@Override
	public JournalStore nextStore(long lastTxId) {
		VersionedFile txFile = parseVersionedFile(nextVersionFile(baseDir, TRANSACTION_PREFIX, lastTxId));
		VersionedFile evnFile = parseVersionedFile(nextVersionFile(baseDir, EVENTS_PREFIX, lastTxId));

		if (txFile.getVersion() != evnFile.getVersion())
			throw new RuntimeException(String.format(
					"Versions of Journals are not equal [tx=%d,evn=%d]",
					txFile.getVersion(), evnFile.getVersion()));

		return store(txFile, evnFile, "");
	}

	@Override
	public synchronized JournalStore nextVolume(long txSize, long eventsSize) {
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
		LOG.info("Finished preallocating journal [{}]", txFile.getName());
		preallocateFiles(new File(baseDir, evnFile.getName()), eventsSize);
		LOG.info("Finished preallocating journal [{}]", evnFile.getName());

		return new JournalStore(txFile.getName(), evnFile.getName(), txFile.getVersion(), 0);
	}

	@Override
	public JournalStore convertVolumeToStore(JournalStore volume) {
		return convertVolumeToStore(volume, 0);
	}

	@Override
	public JournalStore convertVolumeToStore(JournalStore volume, long lastTxId) {
		VersionedFile txFile = parseVersionedFile(nextVersionFile(baseDir, TRANSACTION_PREFIX, lastTxId));
		VersionedFile evnFile = parseVersionedFile(nextVersionFile(baseDir, EVENTS_PREFIX, lastTxId));

		new File(baseDir, volume.getTransactionCommitsAddress()).renameTo(new File(baseDir, txFile.getName()));
		new File(baseDir, volume.getEventsCommitsAddress()).renameTo(new File(baseDir, evnFile.getName()));

		return new JournalStore(txFile.getName(), evnFile.getName(), txFile.getVersion(), lastTxId);
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

	public File getBaseDir() {
		return baseDir;
	}

	protected void merge(List<String> fromStream, String to) {
		if (fromStream.size() == 1) {
			new File(baseDir, fromStream.get(0)).renameTo(new File(baseDir, to));
		} else {
			try {
				final java.nio.channels.FileChannel dest = new RandomAccessFile(new File(baseDir, to), "rw").getChannel();
				final long[] offset = {0};
				fromStream.forEach(f -> {
					try {
						java.nio.channels.FileChannel from = new RandomAccessFile(new File(baseDir, f), "rw").getChannel();
						dest.transferFrom(from, offset[0], from.size());
						offset[0] += from.size();
						from.close();
						new File(baseDir, f).delete();
					} catch (Throwable e) {
						throw Exceptions.runtime(e);
					}
				});
				dest.close();
			} catch (Throwable e) {
				throw Exceptions.runtime(e);
			}
		}
	}

	protected JournalStore[] getJournalStores(List<VersionedFile> txs, List<VersionedFile> evns) {
		LOG.debug("evns: " + evns.size() + ", txs: " + txs.size());
		List<JournalStore> collect = txs.stream().map(tx -> evns.stream()
				.filter(e -> e.getVersion() == tx.getVersion() && e.getFileDate().equals(tx.getFileDate()))
				.map(e -> store(tx, e, "")).findFirst().get()).collect(Collectors.toList());
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
	
	protected List<VersionedFile> afterLastSnapshot(long version, List<VersionedFile> txs) {
		return txs.stream().filter(f -> f.getVersion() > version).collect(Collectors.toList());
	}
	
	protected JournalStore store(VersionedFile txFile, VersionedFile evnFile, String prefix) {
		try {
			new File(baseDir, prefix + txFile.getName()).createNewFile();
			new File(baseDir, prefix + evnFile.getName()).createNewFile();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		long lastTxId = 0L;
		if (txFile.getRest().length > 0) {
			lastTxId = Long.parseLong(txFile.getRest()[0]);
		} else if (txFile.getRest().length != evnFile.getRest().length) {
			throw new IllegalArgumentException("Transaction and Event file names are not equal!");
		}
		return new JournalStore(prefix + txFile.getName(), prefix + evnFile.getName(), txFile.getVersion(), lastTxId);
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
			throw Exceptions.runtime(e);
		}
	}

	public FileSystemStorage(File baseDir, RevenoJournalingConfiguration config) {
		if (!baseDir.exists()) {
			baseDir.mkdirs();
		}
		this.baseDir = baseDir;
		this.config = config;
	}

	protected final File baseDir;
	protected final RevenoJournalingConfiguration config;
	protected static final String TRANSACTION_PREFIX = "tx";
	protected static final String SNAPSHOT_PREFIX = "snp";
	protected static final String EVENTS_PREFIX = "evn";
	protected static final String VOLUME_TRANSACTION_PREFIX = "v_" + TRANSACTION_PREFIX;
	protected static final String VOLUME_EVENTS_PREFIX = "v_" + EVENTS_PREFIX;

	protected static final int PAGE_SIZE = UnsafeUtils.getUnsafe().pageSize();
	protected static final byte[] BLANK_PAGE = new byte[PAGE_SIZE];

	protected static final Logger LOG = LoggerFactory.getLogger(FileSystemStorage.class);
}
