package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

public class GuiMinimapControls extends GuiScreenMinimap {
    protected String screenTitle = "Controls";
    private GuiListKeys keymapList;

    public GuiMinimapControls(Screen parent) {
        this.lastScreen = parent;
    }

    public void init() {
        this.addRenderableWidget(new Button.Builder(Component.translatable("gui.done"), button -> this.onClose()).bounds(this.getWidth() / 2 - 100, this.getHeight() - 26, 200, 20).build());
        this.screenTitle = I18n.get("key.category.voxelmap.controls.title");

        this.keymapList = new GuiListKeys(this);
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

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(this.getFont(), I18n.get("controls.minimap.unbind1"), this.getWidth() / 2, this.getHeight() - 62, 0xFFFFFFFF);
        graphics.centeredText(this.getFont(), "§e" + I18n.get("controls.minimap.unbind2"), this.getWidth() / 2, this.getHeight() - 46, 0xFFFFFFFF);
        graphics.centeredText(this.getFont(), this.screenTitle, this.getWidth() / 2, 20, 0xFFFFFFFF);
    }
}