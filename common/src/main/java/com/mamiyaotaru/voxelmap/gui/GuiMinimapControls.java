package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

public class GuiMinimapControls extends GuiScreenMinimap {
    private GuiListKeys keymapList;

    public GuiMinimapControls(Screen parentGui) {
        super(parentGui, Component.translatable("key.category.voxelmap.controls.title"));
    }

    public void init() {
        addRenderableWidget(keymapList = new GuiListKeys(this, 0, 40, getWidth(), getHeight() - 114));
        addRenderableWidget(new Button.Builder(Component.translatable("gui.done"), button -> onClose()).bounds(getWidth() / 2 - 100, getHeight() - 26, 200, 20).build());
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (keymapList.isEditing()) {
            return keymapList.keyPressed(keyEvent);
        }

        return super.keyPressed(keyEvent);
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);
        guiGraphics.drawCenteredString(getFont(), I18n.get("controls.minimap.unbind1"), getWidth() / 2, getHeight() - 62, 0xFFFFFFFF);
        guiGraphics.drawCenteredString(getFont(), "§e" + I18n.get("controls.minimap.unbind2"), getWidth() / 2, getHeight() - 46, 0xFFFFFFFF);
    }
}