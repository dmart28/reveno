package org.reveno.atp.core.snapshots;

import org.reveno.atp.api.RepositorySnapshotter;
import org.reveno.atp.core.api.storage.SnapshotStorage;
import org.reveno.atp.core.serialization.DefaultJavaSerializer;
import org.reveno.atp.core.serialization.ProtostuffSerializer;

import java.util.ArrayList;
import java.util.List;

public class SnapshottersManager {
	protected volatile List<RepositorySnapshotter> snapshotters = new ArrayList<>();

	public SnapshottersManager(SnapshotStorage storage, ClassLoader classLoader) {
		ProtostuffSerializer protostuffSerializer = new ProtostuffSerializer(classLoader);
		DefaultJavaSerializer javaSerializer = new DefaultJavaSerializer(classLoader);
		snapshotters.add(new DefaultSnapshotter(storage, javaSerializer, protostuffSerializer));
	}

	public void registerSnapshotter(RepositorySnapshotter snapshotter) {
		snapshotters.add(snapshotter);
	}
	
	public void resetSnapshotters() {
		snapshotters = new ArrayList<>();
	}
	
	public List<RepositorySnapshotter> getAll() {
		return snapshotters;
	}
	
}
