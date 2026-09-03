package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.persistent.VoxelMapMigration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public final class VoxelMapMigrationFlow {

    public interface Handler {
        void onKeepLegacy();

        void onMigrated();

        void resolved();
    }

    private VoxelMapMigrationFlow() {}

    public static void start(Screen parent, Component title, Component description, VoxelMapMigration job, boolean showKeepLegacy, Handler handler) {
        start(parent, title, description, job, showKeepLegacy, handler, null);
    }

    public static void start(Screen parent, Component title, Component description, VoxelMapMigration job, boolean showKeepLegacy, Handler handler, @Nullable Runnable onCancel) {
        GuiDataMigration dialog = new GuiDataMigration(parent, title, description, showKeepLegacy, choice -> onChoice(title, job, handler, choice), onCancel);
        VoxelConstants.getMinecraft().gui.setScreen(dialog);
    }

    private static void onChoice(Component title, VoxelMapMigration job, Handler handler, GuiDataMigration.Choice choice) {
        switch (choice) {
            case KEEP_LEGACY -> {
                handler.onKeepLegacy();
                handler.resolved();
            }
            case DELETE -> {
                job.deleteLegacy();
                handler.onMigrated();
                handler.resolved();
            }
            case COPY -> startCopy(title, job, handler);
        }
    }

    private static void startCopy(Component title, VoxelMapMigration job, Handler handler) {
        Minecraft minecraft = VoxelConstants.getMinecraft();
        VoxelMapMigration.Progress progress = new VoxelMapMigration.Progress();
        GuiMigrationProgress progressScreen = new GuiMigrationProgress(title, progress);
        minecraft.gui.setScreen(progressScreen);

        job.copyAsync(progress, () -> minecraft.execute(() -> {
            if (progress.failed) {
                handler.onKeepLegacy();
                handler.resolved();
                return;
            }

            promptDeleteOld(job, handler);
        }));
    }

    private static void promptDeleteOld(VoxelMapMigration job, Handler handler) {
        ConfirmScreen prompt = new ConfirmScreen(deleteOld -> {
            if (deleteOld) {
                job.deleteLegacy();
            }

            handler.onMigrated();
            handler.resolved();
        }, Component.translatable("voxelmap.migration.postCopy.title"), Component.translatable("voxelmap.migration.postCopy.text"),
                Component.translatable("gui.yes"), Component.translatable("gui.no"));
        VoxelConstants.getMinecraft().gui.setScreen(prompt);
    }
}
