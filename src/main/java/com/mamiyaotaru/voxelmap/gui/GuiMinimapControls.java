package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class GuiMinimapControls extends GuiScreenMinimap {
    private final Screen parentScreen;
    protected String screenTitle = "Controls";
    private final MapSettingsManager options;
    public KeyBinding buttonId;

    public GuiMinimapControls(Screen par1GuiScreen) {
        this.parentScreen = par1GuiScreen;
        this.options = VoxelConstants.getVoxelMapInstance().getMapOptions();
    }

    private int getLeftBorder() {
        return this.getWidth() / 2 - 155;
    }

    public void init() {
        int left = this.getLeftBorder();

        for (int t = 0; t < this.options.keyBindings.length; ++t) {
            int id = t;
            this.addDrawableChild(new ButtonWidget.Builder(this.options.getKeybindDisplayString(t), button -> this.controlButtonClicked(id)).dimensions(left + t % 2 * 160, this.getHeight() / 6 + 24 * (t >> 1), 70, 20).build());
        }

        this.addDrawableChild(new ButtonWidget.Builder(Text.translatable("gui.done"), button -> VoxelConstants.getMinecraft().setScreen(this.parentScreen)).dimensions(this.getWidth() / 2 - 100, this.getHeight() / 6 + 168, 200, 20).build());
        this.screenTitle = I18n.translate("controls.minimap.title");
    }

    protected void controlButtonClicked(int id) {
        for (int buttonListIndex = 0; buttonListIndex < this.options.keyBindings.length; ++buttonListIndex) {
            ((ButtonWidget) this.getButtonList().get(buttonListIndex)).setMessage(this.options.getKeybindDisplayString(buttonListIndex));
        }

        this.buttonId = this.options.keyBindings[id];
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.buttonId != null) {
            this.options.setKeyBinding(this.buttonId, InputUtil.Type.MOUSE.createFromCode(button));
            this.buttonId = null;
            KeyBinding.updateKeysByCode();
            return true;
        } else {
            return super.mouseClicked(mouseX, mouseY, button);
        }
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.buttonId != null) {
            if (keyCode == 256) {
                this.options.setKeyBinding(this.buttonId, InputUtil.UNKNOWN_KEY);
            } else {
                this.options.setKeyBinding(this.buttonId, InputUtil.fromKeyCode(keyCode, scanCode));
            }

            this.buttonId = null;
            KeyBinding.updateKeysByCode();
            return true;
        } else {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.drawMap(matrices);
        this.renderBackground(matrices);
        drawCenteredTextWithShadow(matrices, this.getFontRenderer(), this.screenTitle, this.getWidth() / 2, 20, 16777215);
        int leftBorder = this.getLeftBorder();

        for (int keyCounter = 0; keyCounter < this.options.keyBindings.length; ++keyCounter) {
            boolean keycodeCollision = false;
            KeyBinding keyBinding = this.options.keyBindings[keyCounter];

            for (int compareKeyCounter = 0; compareKeyCounter < VoxelConstants.getMinecraft().options.allKeys.length; ++compareKeyCounter) {
                if (compareKeyCounter < this.options.keyBindings.length) {
                    KeyBinding compareBinding = this.options.keyBindings[compareKeyCounter];
                    if (keyBinding != compareBinding && keyBinding.equals(compareBinding)) {
                        keycodeCollision = true;
                        break;
                    }
                }

                KeyBinding compareBinding = VoxelConstants.getMinecraft().options.allKeys[compareKeyCounter];
                if (keyBinding != compareBinding && keyBinding.equals(compareBinding)) {
                    keycodeCollision = true;
                    break;
                }
            }

            if (this.buttonId == this.options.keyBindings[keyCounter]) {
                ((ButtonWidget) this.getButtonList().get(keyCounter)).setMessage((Text.literal("> ")).append((Text.literal("???")).copy().formatted(Formatting.YELLOW)).append(" <").formatted(Formatting.YELLOW));
            } else if (keycodeCollision) {
                ((ButtonWidget) this.getButtonList().get(keyCounter)).setMessage(this.options.getKeybindDisplayString(keyCounter).copy().formatted(Formatting.RED));
            } else {
                ((ButtonWidget) this.getButtonList().get(keyCounter)).setMessage(this.options.getKeybindDisplayString(keyCounter));
            }

            drawTextWithShadow(matrices, this.getFontRenderer(), this.options.getKeyBindingDescription(keyCounter), leftBorder + keyCounter % 2 * 160 + 70 + 6, this.getHeight() / 6 + 24 * (keyCounter >> 1) + 7, -1);
        }

        drawCenteredTextWithShadow(matrices, this.getFontRenderer(), I18n.translate("controls.minimap.unbind1"), this.getWidth() / 2, this.getHeight() / 6 + 115, 16777215);
        drawCenteredTextWithShadow(matrices, this.getFontRenderer(), I18n.translate("controls.minimap.unbind2"), this.getWidth() / 2, this.getHeight() / 6 + 129, 16777215);
        super.render(matrices, mouseX, mouseY, delta);
    }
}
