package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class GuiDataMigration extends GuiScreenMinimap {
    public enum Choice {
        COPY,
        KEEP_LEGACY,
        DELETE
    }

    private final Component titleText;
    private final Component descriptionText;
    private final Consumer<Choice> onChoice;
    private final Runnable onCancel;
    private final boolean showKeepLegacy;

    public GuiDataMigration(Screen parent, Component titleText, Component descriptionText, boolean showKeepLegacy, Consumer<Choice> onChoice, Runnable onCancel) {
        this.lastScreen = parent;
        this.titleText = titleText;
        this.descriptionText = descriptionText;
        this.showKeepLegacy = showKeepLegacy;
        this.onChoice = onChoice;
        this.onCancel = onCancel;
    }

    @Override
    public void init() {
        this.clearWidgets();
        int centerX = this.getWidth() / 2;
        int buttonY = this.getHeight() / 2;

        int offset = 0;
        this.addRenderableWidget(new Button.Builder(Component.translatable("voxelmap.migration.option.copy"), button -> this.onChoice.accept(Choice.COPY))
                .bounds(centerX - 155, buttonY + offset, 310, 20).build());
        offset += 24;

        if (this.showKeepLegacy) {
            this.addRenderableWidget(new Button.Builder(Component.translatable("voxelmap.migration.option.keepLegacy"), button -> this.onChoice.accept(Choice.KEEP_LEGACY))
                    .bounds(centerX - 155, buttonY + offset, 310, 20).build());
            offset += 24;
        }

        this.addRenderableWidget(new Button.Builder(Component.translatable("voxelmap.migration.option.delete"), button -> this.confirmDelete())
                .bounds(centerX - 155, buttonY + offset, 310, 20).build());

        this.addRenderableWidget(new Button.Builder(Component.translatable("gui.cancel"), button -> this.onClose())
                .bounds(centerX - 100, this.getHeight() - 30, 200, 20).build());
    }

    private void confirmDelete() {
        Component title = Component.translatable("voxelmap.migration.deleteConfirm.title");
        Component explanation = Component.translatable("voxelmap.migration.deleteConfirm.text");
        ConfirmScreen confirmScreen = new ConfirmScreen(confirmed -> {
            if (confirmed) {
                this.onChoice.accept(Choice.DELETE);
            } else {
                VoxelConstants.getMinecraft().gui.setScreen(this);
            }
        }, title, explanation, Component.translatable("selectServer.deleteButton"), Component.translatable("gui.cancel"));
        VoxelConstants.getMinecraft().gui.setScreen(confirmScreen);
    }

    @Override
    public void onClose() {
        if (this.onCancel != null) {
            this.onCancel.run();
        } else {
            super.onClose();
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return this.onCancel != null;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int centerX = this.getWidth() / 2;
        graphics.centeredText(this.getFont(), this.titleText, centerX, this.getHeight() / 4 - 20, 0xFFFFFFFF);
        graphics.textWithWordWrap(this.getFont(), this.descriptionText, centerX - 155, this.getHeight() / 4, 310, 0xFFFFFFFF);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }
}
