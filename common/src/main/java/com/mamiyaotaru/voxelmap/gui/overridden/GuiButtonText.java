package com.mamiyaotaru.voxelmap.gui.overridden;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class GuiButtonText extends Button.Plain {
    private boolean editing;
    private final EditBox textField;

    public GuiButtonText(Font fontRenderer, int x, int y, int width, int height, Component message, OnPress onPress) {
        super (x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.textField = new EditBox(fontRenderer, x + 1, y + 1, width - 2, height - 2, null);
    }


    @Override
    public void renderContents(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        if (editing) {
            textField.render(drawContext, mouseX, mouseY, delta);
            return;
        }
        super.renderContents(drawContext, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
        boolean pressed = super.mouseClicked(mouseButtonEvent, doubleClick);
        this.setEditing(pressed);
        return pressed;
    }

    public void setEditing(boolean editing) {
        this.editing = editing;
        if (editing) {
            this.setFocused(true);
        }

        textField.setFocused(editing);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        int keyCode = keyEvent.key();
        if (!(editing)) {
            return super.keyPressed(keyEvent);
        }
        if (keyCode != 257 && keyCode != 335 && keyCode != 258) {
            return textField.keyPressed(keyEvent);
        }

        setEditing(false);
        return false;
    }


    @Override
    public boolean charTyped(CharacterEvent characterEvent) {
        if (!(editing)) {
            return super.charTyped(characterEvent);
        }
        if (characterEvent.codepoint() != '\r') {
            return textField.charTyped(characterEvent);
        }

        setEditing(false);
        return false;
    }

    public boolean isEditing() { return editing; }

    public void setText(String text) { textField.setValue(text); }

    public String getText() { return textField.getValue(); }
}