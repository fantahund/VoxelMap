package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

public class GuiMinimapControls extends GuiScreenMinimap {
    private final Screen parentScreen;
    protected String screenTitle = "Controls";
    private final MapSettingsManager options;
    public KeyMapping buttonId;

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
            this.addRenderableWidget(new Button.Builder(this.options.getKeybindDisplayString(t), button -> this.controlButtonClicked(id)).bounds(left + t % 2 * 160, this.getHeight() / 6 + 24 * (t >> 1), 70, 20).build());
        }

        this.addRenderableWidget(new Button.Builder(Component.translatable("gui.done"), button -> VoxelConstants.getMinecraft().setScreen(this.parentScreen)).bounds(this.getWidth() / 2 - 100, this.getHeight() / 6 + 168, 200, 20).build());
        this.screenTitle = I18n.get("controls.minimap.title");
    }

    protected void controlButtonClicked(int id) {
        for (int buttonListIndex = 0; buttonListIndex < this.options.keyBindings.length; ++buttonListIndex) {
            ((Button) this.getButtonList().get(buttonListIndex)).setMessage(this.options.getKeybindDisplayString(buttonListIndex));
        }

        this.buttonId = this.options.keyBindings[id];
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.buttonId != null) {
            this.options.setKeyBinding(this.buttonId, InputConstants.Type.MOUSE.getOrCreate(button));
            this.buttonId = null;
            KeyMapping.resetMapping();
            return true;
        } else {
            return super.mouseClicked(mouseX, mouseY, button);
        }
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.buttonId != null) {
            if (keyCode == 256) {
                this.options.setKeyBinding(this.buttonId, InputConstants.UNKNOWN);
            } else {
                this.options.setKeyBinding(this.buttonId, InputConstants.getKey(keyCode, scanCode));
            }

            this.buttonId = null;
            KeyMapping.resetMapping();
            return true;
        } else {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        this.renderTransparentBackground(drawContext);
        drawContext.flush();
        drawContext.drawCenteredString(this.getFontRenderer(), this.screenTitle, this.getWidth() / 2, 20, 16777215);
        int leftBorder = this.getLeftBorder();

        for (int keyCounter = 0; keyCounter < this.options.keyBindings.length; ++keyCounter) {
            boolean keycodeCollision = false;
            KeyMapping keyBinding = this.options.keyBindings[keyCounter];

            for (int compareKeyCounter = 0; compareKeyCounter < VoxelConstants.getMinecraft().options.keyMappings.length; ++compareKeyCounter) {
                if (compareKeyCounter < this.options.keyBindings.length) {
                    KeyMapping compareBinding = this.options.keyBindings[compareKeyCounter];
                    if (keyBinding != compareBinding && keyBinding.same(compareBinding)) {
                        keycodeCollision = true;
                        break;
                    }
                }

                KeyMapping compareBinding = VoxelConstants.getMinecraft().options.keyMappings[compareKeyCounter];
                if (keyBinding != compareBinding && keyBinding.same(compareBinding)) {
                    keycodeCollision = true;
                    break;
                }
            }

            if (this.buttonId == this.options.keyBindings[keyCounter]) {
                ((Button) this.getButtonList().get(keyCounter)).setMessage((Component.literal("> ")).append((Component.literal("???")).copy().withStyle(ChatFormatting.YELLOW)).append(" <").withStyle(ChatFormatting.YELLOW));
            } else if (keycodeCollision) {
                ((Button) this.getButtonList().get(keyCounter)).setMessage(this.options.getKeybindDisplayString(keyCounter).copy().withStyle(ChatFormatting.RED));
            } else {
                ((Button) this.getButtonList().get(keyCounter)).setMessage(this.options.getKeybindDisplayString(keyCounter));
            }

            drawContext.drawString(this.getFontRenderer(), this.options.getKeyBindingDescription(keyCounter), leftBorder + keyCounter % 2 * 160 + 70 + 6, this.getHeight() / 6 + 24 * (keyCounter >> 1) + 7, -1);
        }

        drawContext.drawCenteredString(this.getFontRenderer(), I18n.get("controls.minimap.unbind1"), this.getWidth() / 2, this.getHeight() / 6 + 115, 16777215);
        drawContext.drawCenteredString(this.getFontRenderer(), I18n.get("controls.minimap.unbind2"), this.getWidth() / 2, this.getHeight() / 6 + 129, 16777215);
        super.render(drawContext, mouseX, mouseY, delta);
    }
}
