package org.reveno.atp.utils;

import org.reveno.atp.api.exceptions.IllegalFileName;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
 * File name pattern:
 *
 * {prefix}-{date}-{version}{rest}
 *
 * Example:
 *
 * /var/transactions/tx-2015_05_21-14-1
 *
 */
public abstract class VersionedFileUtils {
    private static final String LONG_FORMAT = "%020d";

    public static String nextVersionFile(File baseDir, String prefix) {
        return nextVersionFile(baseDir, prefix, null, "");
    }

    public static String nextVersionFile(File baseDir, String prefix, long lastTransactionId) {
        return nextVersionFile(baseDir, prefix, null, String.format(LONG_FORMAT, lastTransactionId));
    }

    public static String nextVersionFile(File baseDir, String prefix, String version, long lastTransactionId) {
        return nextVersionFile(baseDir, prefix, version, String.format(LONG_FORMAT, lastTransactionId));
    }

    public static String nextVersionFile(File baseDir, String prefix, String version, String rest) {
        if (rest != null && !rest.isEmpty()) {
            rest = "-" + rest;
        }
        final String restStr = rest;
        Optional<String> lastFile = listFiles(baseDir, prefix, false).stream().reduce((a, b) -> b);

        Function<Long, String> nextFile = v -> String.format("%s-%s-%s%s", prefix, format().format(new Date()),
                version == null ? String.format(LONG_FORMAT, v + 1) : String.format(LONG_FORMAT, Long.parseLong(version)), restStr);
        if (!lastFile.isPresent()) {
            return nextFile.apply(0L);
        } else {
            VersionedFile file = parseVersionedFile(lastFile.get());
            if (file.getFileDate().equals(now())) {
                return nextFile.apply(file.getVersion());
            } else {
                if (daysBetween(file.getFileDate(), now()) >= 0) {
                    return nextFile.apply(file.getVersion());
                } else {
                    throw new RuntimeException("Your system clock is out of sync with transaction data. Please check it.");
                }
            }
        }
    }

    public static Optional<String> lastVersionFile(File baseDir, String prefix) {
        return listFiles(baseDir, prefix, false).stream().reduce((a, b) -> b);
    }

    public static VersionedFile lastVersionedFile(File baseDir, String prefix) {
        Optional<String> o = lastVersionFile(baseDir, prefix);
        return o.map(VersionedFileUtils::parseVersionedFile).orElse(null);
    }

    public static VersionedFile parseVersionedFile(String fileName) {
        String[] parts = fileName.split("-");
        if (parts.length < 3)
            throw new IllegalFileName(fileName, null);

        try {
            Calendar c = Calendar.getInstance();
            c.setTime(format().parse(parts[1]));
            return new VersionedFile(fileName, parts[0], c, Long.parseLong(parts[2]), produceRest(parts));
        } catch (Throwable t) {
            throw new IllegalFileName(fileName, t);
        }
    }

    public static List<String> listFolders(File baseDir, String prefix) {
        return listFiles(baseDir.getAbsolutePath())
                .filter(File::isDirectory)
                .map(File::getName)
                .filter(fn -> fn.startsWith(prefix))
                .sorted()
                .collect(Collectors.toList());
    }

    public static List<String> listFiles(File baseDir, String prefix, boolean listToday) {
        return listFiles(baseDir.getAbsolutePath())
                .filter(File::isFile)
                .map(File::getName)
                .filter(fn -> fn.startsWith(prefix + (listToday ? "-" + format().format(new Date()) : "")))
                .sorted()
                .collect(Collectors.toList());
    }

    public static List<VersionedFile> listVersioned(File baseDir, String prefix) {
        return listFiles(baseDir, prefix, false).stream().map(VersionedFileUtils::parseVersionedFile)
                .sorted(VersionedFileUtils::versionedSorter).collect(Collectors.toList());
    }

    protected static Stream<File> listFiles(String directory) {
        List<File> list = new ArrayList<>();
        File folder = new File(directory);
        File[] listOfFiles = folder.listFiles();

        Collections.addAll(list, listOfFiles == null ? new File[0] : listOfFiles);
        return list.stream();
    }

    protected static int versionedSorter(VersionedFile a, VersionedFile b) {
        if (a.getVersion() < b.getVersion()) return -1;
        if (a.getVersion() > b.getVersion()) return 1;
        return 0;
    }

    protected static long daysBetween(Calendar startDate, Calendar endDate) {
        long end = endDate.getTimeInMillis();
        long start = startDate.getTimeInMillis();
        return TimeUnit.MILLISECONDS.toDays(Math.abs(end - start));
    }

    protected static Calendar now() {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        return c;
    }

    protected static String[] produceRest(String[] parts) {
        String[] rest = new String[parts.length - 3];
        System.arraycopy(parts, 3, rest, 0, parts.length - 3);
        return rest;
    }

    protected static SimpleDateFormat format() {
        return new SimpleDateFormat("yyyy_MM_dd");
    }

    public static class VersionedFile {
        private String name;
        private String prefix;
        private Calendar fileDate;
        private long version;
        private String[] rest;

        public VersionedFile(String name, String prefix, Calendar fileDate, long version, String[] rest) {
            this.name = name;
            this.prefix = prefix;
            this.fileDate = fileDate;
            this.version = version;
            this.rest = rest;
        }

        public String getName() {
            return name;
        }

        public String getPrefix() {
            return prefix;
        }

        public Calendar getFileDate() {
            return fileDate;
        }

        public long getVersion() {
            return version;
        }

        public String[] getRest() {
            return rest;
        }

        @Override
        public String toString() {
            return String.format("[%s,%s]", name, version);
        }
    }
}
