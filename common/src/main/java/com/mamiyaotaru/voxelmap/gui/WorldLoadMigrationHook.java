package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.persistent.VoxelMapDataConfig;
import com.mamiyaotaru.voxelmap.persistent.VoxelMapMigration;
import java.io.File;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.network.chat.Component;

public final class WorldLoadMigrationHook {
    private static boolean bypass;

    private WorldLoadMigrationHook() {}

    public static boolean interceptWorldLoad(WorldOpenFlows flows, String levelId, Runnable onFail) {
        if (bypass) {
            return false;
        }

        Minecraft minecraft = VoxelConstants.getMinecraft();
        File saveFolder;
        try {
            saveFolder = minecraft.getLevelSource().getLevelPath(levelId).toFile();
        } catch (RuntimeException e) {
            VoxelConstants.getLogger().error("Could not resolve save folder for " + levelId + ", skipping migration check", e);
            return false;
        }

        String saveId = saveFolder.getName();
        VoxelMapDataConfig config = VoxelMapDataConfig.getInstance();

        if (config.getWorldDecision(saveId) != null) {
            return false;
        }

        if (VoxelMapMigration.worldRelativeDataExists(saveFolder)) {
            config.setWorldDecision(saveId, VoxelMapDataConfig.Decision.MIGRATED);
            return false;
        }

        VoxelMapMigration job = VoxelMapMigration.forSingleplayer(saveFolder, saveId);
        if (!job.hasData()) {
            config.setWorldDecision(saveId, VoxelMapDataConfig.Decision.MIGRATED);
            return false;
        }

        Screen parent = minecraft.gui.screen();
        Component title = Component.translatable("voxelmap.migration.singleplayer.title");
        Component description = Component.translatable("voxelmap.migration.singleplayer.description", saveId);
        VoxelMapMigrationFlow.start(parent, title, description, job, false, new VoxelMapMigrationFlow.Handler() {
            @Override
            public void onKeepLegacy() {
            }

            @Override
            public void onMigrated() {
                config.setWorldDecision(saveId, VoxelMapDataConfig.Decision.MIGRATED);
            }

            @Override
            public void resolved() {
                resumeWorldLoad(flows, levelId, onFail);
            }
        });

        return true;
    }

    private static void resumeWorldLoad(WorldOpenFlows flows, String levelId, Runnable onFail) {
        bypass = true;
        try {
            flows.openWorld(levelId, onFail);
        } finally {
            bypass = false;
        }
    }
}
