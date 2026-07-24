package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import com.mamiyaotaru.voxelmap.persistent.VoxelMapMigration;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class GuiMigrationProgress extends GuiScreenMinimap {
    private final Component titleText;
    private final VoxelMapMigration.Progress progress;

    public GuiMigrationProgress(Component titleText, VoxelMapMigration.Progress progress) {
        this.titleText = titleText;
        this.progress = progress;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    private static String humanBytes(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }

        if (bytes < 1024L * 1024L) {
            return String.format("%.1f KB", bytes / 1024.0);
        }

        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int centerX = this.getWidth() / 2;
        int barWidth = 300;
        int barHeight = 10;
        int barX = centerX - barWidth / 2;
        int barY = this.getHeight() / 2 - barHeight / 2;

        graphics.centeredText(this.getFont(), this.titleText, centerX, barY - 40, 0xFFFFFFFF);

        long total = this.progress.totalBytes;
        long copied = this.progress.copiedBytes;
        float fraction = total > 0L ? Math.min(1.0F, (float) copied / (float) total) : (this.progress.done ? 1.0F : 0.0F);

        graphics.fill(barX - 1, barY - 1, barX + barWidth + 1, barY + barHeight + 1, 0xFF000000);
        graphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF555555);
        graphics.fill(barX, barY, barX + (int) (barWidth * fraction), barY + barHeight, 0xFF4CAF50);

        String status = this.progress.copiedFiles + " / " + this.progress.totalFiles + " "
                + Component.translatable("voxelmap.migration.progress.files").getString()
                + "  (" + humanBytes(copied) + " / " + humanBytes(total) + ")";
        graphics.centeredText(this.getFont(), status, centerX, barY + barHeight + 8, 0xFFA0A0A0);

        if (!this.progress.currentFile.isEmpty() && !this.progress.done) {
            graphics.centeredText(this.getFont(), this.progress.currentFile, centerX, barY + barHeight + 20, 0xFF808080);
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }
}
