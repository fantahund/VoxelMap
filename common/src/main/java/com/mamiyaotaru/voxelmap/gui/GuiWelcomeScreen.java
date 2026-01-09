package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;

public class GuiWelcomeScreen extends GuiScreenMinimap {
    private Screen parent;
    private MapSettingsManager options;

    private PlainTextButton closeButton;
    private PlainTextButton controlsButton;

    private final ArrayList<Component> welcomeTexts = new ArrayList<>();

    public GuiWelcomeScreen(Screen parentScreen) {
        this.parent = parentScreen;
        this.setParentScreen(this.parent);

        this.options = VoxelConstants.getVoxelMapInstance().getMapOptions();
    }

    @Override
    public void init() {
        this.clearWidgets();

        String maintainers = "Fantahund, Brokkonaut, Gamja";
        this.welcomeTexts.clear();
        this.welcomeTexts.add(Component.literal("VoxelMap!").withStyle(ChatFormatting.RED));
        this.welcomeTexts.add(Component.translatable("minimap.ui.welcome1", maintainers));
        this.welcomeTexts.add(Component.translatable("minimap.ui.welcome2"));
        this.welcomeTexts.add(Component.translatable("minimap.ui.welcome3"));
        this.welcomeTexts.add(Component.translatable("minimap.ui.welcome4"));
        this.welcomeTexts.add(Component.empty());
        KeyMapping[] sortedKeys = Arrays.stream(options.keyBindings).sorted().toArray(KeyMapping[]::new);
        int count = 0;
        for (KeyMapping key : sortedKeys) {
            if (key.isUnbound()) continue;

            Component keyText = Component.empty()
                    .append(key.getTranslatedKeyMessage().copy().withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(": ").withStyle(ChatFormatting.WHITE))
                    .append(Component.translatable(key.getName()));

            if (count % 2 == 0) {
                this.welcomeTexts.add(keyText);
            } else {
                int lastIndex = this.welcomeTexts.size() - 1;
                this.welcomeTexts.set(lastIndex, this.welcomeTexts.getLast().copy().append(", ").append(keyText));
            }

            ++count;
        }

        Component closeThisMessage = Component.translatable("minimap.ui.welcome5").withStyle(ChatFormatting.GRAY);
        this.addRenderableWidget(this.closeButton = new PlainTextButton(getWidth() / 2 + 9, getHeight() / 2, 95, 10, closeThisMessage, button -> {
            this.options.setOptionValue(EnumOptionsMinimap.WELCOME_SCREEN);
            this.options.saveAll();

            minecraft.setScreen(null);
        }, getFont()));

        Component controls = Component.translatable("options.controls").withStyle(ChatFormatting.GRAY);
        this.addRenderableWidget(this.controlsButton = new PlainTextButton(getWidth() / 2 - 104, getHeight() / 2, 95, 10, controls, button -> minecraft.setScreen(new GuiMinimapControls(this)), getFont()));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        int centerX = minecraft.getWindow().getGuiScaledWidth() / 2;
        int centerY = minecraft.getWindow().getGuiScaledHeight() / 2;

        int lineHeight = getFont().lineHeight;

        int boxColor = ARGB.color(128, 0, 0, 0);
        int boxTop = centerY - ((this.welcomeTexts.size() * lineHeight) / 2);

        // Main Box
        int boxWidth = 0;
        for (int i = 1; i < this.welcomeTexts.size(); ++i) {
            boxWidth = Math.max(boxWidth, getFont().width(this.welcomeTexts.get(i)));
        }
        this.drawBox(guiGraphics, centerX - (boxWidth / 2), boxTop, centerX + (boxWidth / 2), boxTop + lineHeight * (this.welcomeTexts.size() - 1), 4, 1, boxColor);

        // Title Box
        this.drawBox(guiGraphics, centerX - (boxWidth / 2), boxTop - lineHeight - 3, centerX + (boxWidth / 2), boxTop + lineHeight * (this.welcomeTexts.size() - 1) + 22, 6, 1, boxColor);

        // Main Text
        for (int i = 1; i < this.welcomeTexts.size(); ++i) {
            guiGraphics.drawString(getFont(), this.welcomeTexts.get(i), centerX - (boxWidth / 2), boxTop + lineHeight * (i - 1), 0xFFFFFFFF);
        }

        //  Title Text
        guiGraphics.drawCenteredString(getFont(), this.welcomeTexts.getFirst(), centerX, boxTop - lineHeight - 3, 0xFFFFFFFF);
        boxTop += lineHeight * this.welcomeTexts.size();


        // Buttons
        this.drawBox(guiGraphics, this.closeButton.getX(), boxTop, this.closeButton.getX() + this.closeButton.getWidth(), boxTop + this.closeButton.getHeight(), 4, 1, boxColor);
        this.closeButton.setY(boxTop);

        this.drawBox(guiGraphics, this.controlsButton.getX(), boxTop, this.controlsButton.getX() + this.controlsButton.getWidth(), boxTop + this.controlsButton.getHeight(), 4, 1, boxColor);
        this.controlsButton.setY(boxTop);

        super.render(guiGraphics, mouseX, mouseY, delta);
    }

    private void drawBox(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int inflateX, int inflateY, int color) {
        guiGraphics.fill(x1 - inflateX, y1 - inflateY, x2 + inflateX, y2 + inflateY, color);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (keyEvent.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.options.setOptionValue(EnumOptionsMinimap.WELCOME_SCREEN);
            this.options.saveAll();
        }

        return super.keyPressed(keyEvent);
    }
}
