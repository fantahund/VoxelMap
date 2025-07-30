package com.mamiyaotaru.voxelmap.gui.overridden;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class GuiButtonText extends Button {
    private boolean editing;
    private final EditBox textField;

    public GuiButtonText(Font fontRenderer, int x, int y, int width, int height, Component message, OnPress onPress) {
        super (x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.textField = new EditBox(fontRenderer, x + 1, y + 1, width - 2, height - 2, null);
    }


    public void renderWidget(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        if (editing) {
            textField.render(drawContext, mouseX, mouseY, delta);
            return;
        }

        super.renderWidget(drawContext, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button, boolean doubleClick) {
        boolean pressed = super.mouseClicked(mouseX, mouseY, button, doubleClick);
        this.setEditing(pressed);
        return pressed;
    }

    public void setEditing(boolean editing) {
        this.editing = editing;
        if (editing) this.setFocused(true);

        textField.setFocused(editing);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!(editing)) return super.keyPressed(keyCode, scanCode, modifiers);
        if (keyCode != 257 && keyCode != 335 && keyCode != 258) return textField.keyPressed(keyCode, scanCode, modifiers);

        setEditing(false);
        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (!(editing)) return super.charTyped(chr, modifiers);
        if (chr != '\r') return textField.charTyped(chr, modifiers);

        setEditing(false);
        return false;
    }

    public boolean isEditing() { return editing; }

    public void setText(String text) { textField.setValue(text); }

    public String getText() { return textField.getValue(); }
}