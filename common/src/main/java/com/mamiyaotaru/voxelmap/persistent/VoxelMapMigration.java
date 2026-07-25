package com.mamiyaotaru.voxelmap.persistent;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.util.TextUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class VoxelMapMigration {

    public static final class Progress {
        public volatile long totalBytes;
        public volatile int totalFiles;
        public final AtomicLong copiedBytes = new AtomicLong();
        public final AtomicInteger copiedFiles = new AtomicInteger();
        public volatile boolean done;
        public volatile boolean failed;
        public volatile String currentFile = "";
    }

    private record Item(File source, File dest, boolean mergeWaypoints) {}

    private final List<Item> items = new ArrayList<>();
    private final List<File> legacySources = new ArrayList<>();
    private final boolean overwrite;

    private VoxelMapMigration(boolean overwrite) {
        this.overwrite = overwrite;
    }

    private void add(File source, File dest) {
        add(source, dest, false);
    }

    private void add(File source, File dest, boolean mergeWaypoints) {
        if (source != null && source.exists()) {
            this.items.add(new Item(source, dest, mergeWaypoints));
            this.legacySources.add(source);
        }
    }

    public boolean hasData() {
        return !this.items.isEmpty();
    }

    public static VoxelMapMigration forSingleplayer(File saveFolder, String worldName) {
        String key = TextUtils.scrubNameFile(worldName);
        File globalRoot = VoxelMapDataStore.getGlobalRoot();
        File targetRoot = VoxelMapDataStore.getWorldRelativeRoot(saveFolder);

        VoxelMapMigration job = new VoxelMapMigration(true);
        job.add(new File(globalRoot, key + ".points"), new File(targetRoot, "way.points"));
        job.add(new File(globalRoot, "cache/" + key), new File(targetRoot, "cache"));
        return job;
    }

    public static VoxelMapMigration forServerMerge(String fromName, String toName) {
        String fromKey = TextUtils.scrubNameFile(fromName);
        String toKey = TextUtils.scrubNameFile(toName);
        File globalRoot = VoxelMapDataStore.getGlobalRoot();

        VoxelMapMigration job = new VoxelMapMigration(false);
        job.add(new File(globalRoot, fromKey + ".points"), new File(globalRoot, toKey + ".points"), true);
        job.add(new File(globalRoot, "cache/" + fromKey), new File(globalRoot, "cache/" + toKey));
        return job;
    }

    public static boolean worldRelativeDataExists(File saveFolder) {
        File root = VoxelMapDataStore.getWorldRelativeRoot(saveFolder);
        if (!root.isDirectory()) {
            return false;
        }

        String[] children = root.list();
        return children != null && children.length > 0;
    }

    private void measure(Progress progress) {
        long[] totals = new long[2];
        for (Item item : this.items) {
            measure(item.source(), totals);
        }

        progress.totalBytes = totals[0];
        progress.totalFiles = (int) totals[1];
    }

    private static void measure(File file, long[] totals) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    measure(child, totals);
                }
            }
        } else if (file.isFile()) {
            totals[0] += file.length();
            totals[1] += 1L;
        }
    }

    public boolean copy(Progress progress) {
        measure(progress);
        try {
            for (Item item : this.items) {
                if (item.mergeWaypoints() && item.source().isFile() && item.dest().exists()) {
                    progress.currentFile = item.source().getName();
                    WaypointFileMerger.mergeInto(item.source(), item.dest());
                    progress.copiedBytes.addAndGet(item.source().length());
                    progress.copiedFiles.incrementAndGet();
                } else {
                    copyRecursive(item.source().toPath(), item.dest().toPath(), progress, this.overwrite);
                }
            }

            progress.done = true;
            return true;
        } catch (IOException e) {
            VoxelConstants.getLogger().error("VoxelMap data migration failed", e);
            progress.failed = true;
            progress.done = true;
            return false;
        }
    }

    private static void copyRecursive(Path source, Path dest, Progress progress, boolean overwrite) throws IOException {
        File sourceFile = source.toFile();
        if (sourceFile.isDirectory()) {
            Files.createDirectories(dest);
            File[] children = sourceFile.listFiles();
            if (children != null) {
                for (File child : children) {
                    copyRecursive(child.toPath(), dest.resolve(child.getName()), progress, overwrite);
                }
            }
        } else if (sourceFile.isFile()) {
            progress.currentFile = sourceFile.getName();

            if (!overwrite && Files.exists(dest)) {
                progress.copiedBytes.addAndGet(sourceFile.length());
                progress.copiedFiles.incrementAndGet();
                return;
            }

            Path parent = dest.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            progress.copiedBytes.addAndGet(sourceFile.length());
            progress.copiedFiles.incrementAndGet();
        }
    }

    public void deleteLegacy() {
        for (File source : this.legacySources) {
            deleteRecursive(source);
        }
    }

    private static void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }

        if (!file.delete() && file.exists()) {
            VoxelConstants.getLogger().warn("Failed to delete legacy VoxelMap data at " + file.getPath());
        }
    }

    public void copyAsync(Progress progress, Runnable onDone) {
        Thread thread = new Thread(() -> {
            copy(progress);
            if (onDone != null) {
                onDone.run();
            }
        }, "VoxelMap Data Migration");
        thread.setDaemon(true);
        thread.start();
    }
}
