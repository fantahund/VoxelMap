package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.VoxelMap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import com.mamiyaotaru.voxelmap.util.I18nUtils;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class GuiMinimapControls extends GuiScreenMinimap {
    private final Screen parentScreen;
    protected String screenTitle = "Controls";
    private final MapSettingsManager options;
    public KeyBinding buttonId = null;

    public GuiMinimapControls(Screen par1GuiScreen, VoxelMap master) {
        this.parentScreen = par1GuiScreen;
        this.options = master.getMapOptions();
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
        this.screenTitle = I18nUtils.getString("controls.minimap.title");
    }

    protected void controlButtonClicked(int id) {
        for (int buttonListIndex = 0; buttonListIndex < this.options.keyBindings.length; ++buttonListIndex) {
            ((ButtonWidget) this.getButtonList().get(buttonListIndex)).setMessage(this.options.getKeybindDisplayString(buttonListIndex));
        }

        this.buttonId = this.options.keyBindings[id];
    }

    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (this.buttonId != null) {
            this.options.setKeyBinding(this.buttonId, InputUtil.Type.MOUSE.createFromCode(mouseButton));
            this.buttonId = null;
            KeyBinding.updateKeysByCode();
            return true;
        } else {
            return super.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    public boolean keyPressed(int keysm, int scancode, int b) {
        if (this.buttonId != null) {
            if (keysm == 256) {
                this.options.setKeyBinding(this.buttonId, InputUtil.UNKNOWN_KEY);
            } else {
                this.options.setKeyBinding(this.buttonId, InputUtil.fromKeyCode(keysm, scancode));
            }

            this.buttonId = null;
            KeyBinding.updateKeysByCode();
            return true;
        } else {
            return super.keyPressed(keysm, scancode, b);
        }
    }

    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.drawMap(matrixStack);
        this.renderBackground(matrixStack);
        drawCenteredText(matrixStack, this.getFontRenderer(), this.screenTitle, this.getWidth() / 2, 20, 16777215);
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

            drawStringWithShadow(matrixStack, this.getFontRenderer(), this.options.getKeyBindingDescription(keyCounter), leftBorder + keyCounter % 2 * 160 + 70 + 6, this.getHeight() / 6 + 24 * (keyCounter >> 1) + 7, -1);
        }

        drawCenteredText(matrixStack, this.getFontRenderer(), I18nUtils.getString("controls.minimap.unbind1"), this.getWidth() / 2, this.getHeight() / 6 + 115, 16777215);
        drawCenteredText(matrixStack, this.getFontRenderer(), I18nUtils.getString("controls.minimap.unbind2"), this.getWidth() / 2, this.getHeight() / 6 + 129, 16777215);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }
}
