package com.mamiyaotaru.voxelmap.gui.overridden;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class GuiButtonText extends ButtonWidget {
    private boolean editing = false;
    private final TextFieldWidget textField;

    public GuiButtonText(TextRenderer fontRenderer, int x, int y, int widthIn, int heightIn, Text buttonText, ButtonWidget.PressAction action) {
        super(x, y, widthIn, heightIn, buttonText, action, DEFAULT_NARRATION_SUPPLIER);
        this.textField = new TextFieldWidget(fontRenderer, x + 1, y + 1, widthIn - 2, heightIn - 2, null);
    }

    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        if (!this.editing) {
            super.render(matrixStack, mouseX, mouseY, partialTicks);
        } else {
            this.textField.render(matrixStack, mouseX, mouseY, partialTicks);
        }

    }

    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        boolean pressed = super.mouseClicked(mouseX, mouseY, mouseButton);
        this.setEditing(pressed);
        return pressed;
    }

    public void setEditing(boolean editing) {
        this.editing = editing;
        if (editing) {
            this.setFocused(true);
        }

        this.textField.setTextFieldFocused(editing);
    }

    public boolean keyPressed(int keysm, int scancode, int b) {
        boolean ok = false;
        if (this.editing) {
            if (keysm != 257 && keysm != 335 && keysm != 258) {
                ok = this.textField.keyPressed(keysm, scancode, b);
            } else {
                this.setEditing(false);
            }
        } else {
            ok = super.keyPressed(keysm, scancode, b);
        }

        return ok;
    }

    public boolean charTyped(char character, int keycode) {
        boolean ok = false;
        if (this.editing) {
            if (character == '\r') {
                this.setEditing(false);
            } else {
                ok = this.textField.charTyped(character, keycode);
            }
        } else {
            ok = super.charTyped(character, keycode);
        }

        return ok;
    }

    public boolean isEditing() {
        return this.editing;
    }

    public void tick() {
        this.textField.tick();
    }

    public void setText(String textIn) {
        this.textField.setText(textIn);
    }

    public String getText() {
        return this.textField.getText();
    }
}
