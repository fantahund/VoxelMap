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

    private static String cacheKey() {
        return TextUtils.scrubNameFile(currentWorldName());
    }

    private static String pointsKey() {
        String name = currentWorldName();
        if (name.endsWith(":25565")) {
            int portSepLoc = name.lastIndexOf(':');
            if (portSepLoc != -1) {
                name = name.substring(0, portSepLoc);
            }
        }

        return TextUtils.scrubNameFile(name);
    }

    public File getPointsFile() {
        return isWorldRelative() ? new File(getRoot(), "way.points") : new File(getRoot(), pointsKey() + ".points");
    }

    public File getWorldCacheDir() {
        File cache = new File(getRoot(), "cache");
        return isWorldRelative() ? cache : new File(cache, cacheKey());
    }
}
