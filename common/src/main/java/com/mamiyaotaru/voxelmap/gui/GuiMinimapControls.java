package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

public class GuiMinimapControls extends GuiScreenMinimap {
    protected String screenTitle = "Controls";
    private GuiButtonRowListKeys keymapList;

    public GuiMinimapControls(Screen parent) {
        this.lastScreen = parent;
    }

    public void init() {
        this.addRenderableWidget(new Button.Builder(Component.translatable("gui.done"), button -> this.onClose()).bounds(this.getWidth() / 2 - 100, this.getHeight() - 28, 200, 20).build());
        this.screenTitle = I18n.get("key.category.voxelmap.controls.title");

        this.keymapList = new GuiButtonRowListKeys(this);
        this.addRenderableWidget(this.keymapList);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (this.keymapList.keyEditing()) {
            return this.keymapList.keyPressed(keyEvent);
        } else {
            return super.keyPressed(keyEvent);
        }
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);
        guiGraphics.drawCenteredString(this.getFont(), I18n.get("controls.minimap.unbind1"), this.getWidth() / 2, this.getHeight() - 64, 0xFFFFFFFF);
        guiGraphics.drawCenteredString(this.getFont(), "§e" + I18n.get("controls.minimap.unbind2"), this.getWidth() / 2, this.getHeight() - 48, 0xFFFFFFFF);
        guiGraphics.drawCenteredString(this.getFont(), this.screenTitle, this.getWidth() / 2, 20, 0xFFFFFFFF);
    }
}