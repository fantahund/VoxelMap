package com.mamiyaotaru.voxelmap.persistent;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.util.TextUtils;
import java.io.File;
import java.util.Optional;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.storage.LevelResource;

public final class VoxelMapDataStore {
    private volatile File root;
    private volatile boolean worldRelative;

    public static File getGlobalRoot() {
        return new File(VoxelConstants.getMinecraft().gameDirectory, "voxelmap");
    }

    public static File getWorldRelativeRoot(File saveFolder) {
        return new File(saveFolder, "data/voxelmap");
    }

    public static Optional<File> getCurrentSaveFolder() {
        Optional<IntegratedServer> server = VoxelConstants.getIntegratedServer();
        return server.map(integratedServer -> integratedServer.getWorldPath(LevelResource.ROOT).normalize().toFile());
    }

    public void resolveForCurrentWorld() {
        computeRoot();
    }

    private File computeRoot() {
        if (VoxelConstants.getMinecraft().hasSingleplayerServer()) {
            Optional<File> saveFolder = getCurrentSaveFolder();
            if (saveFolder.isPresent()) {
                this.worldRelative = true;
                this.root = getWorldRelativeRoot(saveFolder.get());
                return this.root;
            }
        }

        this.worldRelative = false;
        this.root = getGlobalRoot();
        return this.root;
    }

    public File getRoot() {
        File resolved = this.root;
        if (resolved == null) {
            resolved = computeRoot();
        }

        if (!resolved.exists()) {
            resolved.mkdirs();
        }

        return resolved;
    }

    public boolean isWorldRelative() {
        if (this.root == null) {
            computeRoot();
        }

        return this.worldRelative;
    }

    private static String currentWorldName() {
        return VoxelConstants.getVoxelMapInstance().getWaypointManager().getCurrentWorldName();
    }

    private static final String UNKNOWN_WORLD_KEY = "_unknown";

    private static String cacheKey() {
        String key = TextUtils.scrubNameFile(currentWorldName());
        if (key.isEmpty()) {
            VoxelConstants.getLogger().warn("World cache key resolved empty; falling back to " + UNKNOWN_WORLD_KEY + " instead of flattening into the global cache folder");
            return UNKNOWN_WORLD_KEY;
        }

        return key;
    }

    private static String pointsKey() {
        String name = currentWorldName();
        if (name.endsWith(":25565")) {
            int portSepLoc = name.lastIndexOf(':');
            if (portSepLoc != -1) {
                name = name.substring(0, portSepLoc);
            }
        }

        String key = TextUtils.scrubNameFile(name);
        return key.isEmpty() ? UNKNOWN_WORLD_KEY : key;
    }

    public File getPointsFile() {
        return isWorldRelative() ? new File(getRoot(), "way.points") : new File(getRoot(), pointsKey() + ".points");
    }

    public File getWorldCacheDir() {
        File cache = new File(getRoot(), "cache");
        return isWorldRelative() ? cache : new File(cache, cacheKey());
    }

    public File getWorldCacheDir(String subPath) {
        File properDir = new File(getWorldCacheDir(), subPath);
        recoverMisplacedCache(subPath, properDir);
        return properDir;
    }

    private void recoverMisplacedCache(String subPath, File properDir) {
        if (isWorldRelative() || properDir.exists()) {
            return;
        }

        File cacheRoot = new File(getRoot(), "cache");
        File flatDir = new File(cacheRoot, subPath);
        if (!flatDir.isDirectory() || flatDir.equals(properDir)) {
            return;
        }

        File parent = properDir.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }

        if (flatDir.renameTo(properDir)) {
            VoxelConstants.getLogger().warn("Recovered VoxelMap cache data misplaced by a previous bug: moved " + flatDir.getPath() + " to " + properDir.getPath());
            deleteEmptyParents(flatDir.getParentFile(), cacheRoot);
        } else {
            VoxelConstants.getLogger().warn("Found VoxelMap cache data misplaced by a previous bug at " + flatDir.getPath() + " but failed to move it to " + properDir.getPath());
        }
    }

    private static void deleteEmptyParents(File dir, File stopAt) {
        while (dir != null && !dir.equals(stopAt)) {
            String[] children = dir.list();
            if (children == null || children.length > 0) {
                return;
            }

            File parent = dir.getParentFile();
            if (!dir.delete()) {
                return;
            }

            dir = parent;
        }
    }
}
