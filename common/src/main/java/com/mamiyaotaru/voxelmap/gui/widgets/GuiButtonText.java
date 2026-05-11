package com.mamiyaotaru.voxelmap.gui.widgets;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class GuiButtonText extends Button.Plain {
    private final EditBox textField;
    private final Consumer<GuiButtonText> onUpdate;
    private boolean editing;

    public GuiButtonText(Font font, int x, int y, int width, int height, Component message, Consumer<GuiButtonText> onUpdate) {
        super (x, y, width, height, message, (button) -> {}, DEFAULT_NARRATION);
        textField = new EditBox(font, x + 1, y + 1, width - 2, height - 2, Component.empty());
        this.onUpdate = onUpdate;
    }

    public boolean isEditing() {
        return editing;
    }

    public void setEditing(boolean editing) {
        this.editing = editing;
        if (editing) {
            setFocused(true);
        }

        textField.setFocused(editing);
        textField.setX(getX() + 1);
        textField.setY(getY() + 1);
        textField.setWidth(getWidth() - 2);
        textField.setHeight(getHeight() - 2);
    }

    public String getText() {
        return textField.getValue();
    }

    public void setText(String text) {
        textField.setValue(text);
    }

    @Override
    public void onPress(InputWithModifiers input) {
        setEditing(true);
    }

    @Override
    public void renderContents(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        super.renderContents(drawContext, mouseX, mouseY, delta);

        if (editing) {
            textField.render(drawContext, mouseX, mouseY, delta);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
        if (!editing) {
            return super.mouseClicked(mouseButtonEvent, doubleClick);
        }

        return textField.mouseClicked(mouseButtonEvent, doubleClick);
    }

    @Override
    public boolean charTyped(CharacterEvent characterEvent) {
        if (!editing) {
            return super.charTyped(characterEvent);
        }

        return textField.charTyped(characterEvent);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        int keyCode = keyEvent.key();
        if (!editing) {
            return super.keyPressed(keyEvent);
        }

        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_TAB) {
            setEditing(false);
            onUpdate.accept(this);
            return false;
        }

        return textField.keyPressed(keyEvent);
    }
}