package org.reveno.atp.test.utils;

import java.io.File;
import java.io.IOException;

public abstract class FileUtils {

	public static void delete(File f) throws IOException {
		clearFolder(f);
		f.delete();
	}

	public static void clearFolder(File f) throws IOException {
		if (f.isDirectory()) {
			for (File c : f.listFiles())
				delete(c);
		}
	}

	public static File findFile(String folder, String start) throws IOException {
		File dir = new File(folder);
		File[] files = dir.listFiles((f,n) -> n.startsWith(start));
		if (files.length == 0) {
			return null;
		} else {
			return files[0];
		}
	}

}
