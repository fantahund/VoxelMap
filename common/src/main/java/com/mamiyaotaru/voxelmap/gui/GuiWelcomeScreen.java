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
    private MapSettingsManager options;

    private PlainTextButton closeButton;
    private PlainTextButton controlsButton;

    private final ArrayList<Component> welcomeTexts = new ArrayList<>();

    public GuiWelcomeScreen(Screen parentGui) {
        super(parentGui, Component.empty());
        options = VoxelConstants.getVoxelMapInstance().getMapOptions();
    }

    @Override
    public void init() {
        clearWidgets();

        String maintainers = "Fantahund, Brokkonaut, Algamja11";
        welcomeTexts.clear();
        welcomeTexts.add(Component.literal("VoxelMap!").withStyle(ChatFormatting.RED));
        welcomeTexts.add(Component.translatable("minimap.ui.welcome1", maintainers));
        welcomeTexts.add(Component.translatable("minimap.ui.welcome2"));
        welcomeTexts.add(Component.empty());
        welcomeTexts.add(Component.translatable("minimap.ui.welcome3"));
        welcomeTexts.add(Component.translatable("minimap.ui.welcome4"));
        welcomeTexts.add(Component.empty());
        KeyMapping[] sortedKeys = Arrays.stream(options.keyBindings).sorted().toArray(KeyMapping[]::new);
        int count = 0;
        for (KeyMapping key : sortedKeys) {
            if (key.isUnbound()) continue;

            Component keyText = Component.literal("§b").append(key.getTranslatedKeyMessage()).append("§r: ").append(Component.translatable(key.getName()));
            if (count % 2 == 0) {
                welcomeTexts.add(keyText);
            } else {
                int lastIndex = welcomeTexts.size() - 1;
                welcomeTexts.set(lastIndex, welcomeTexts.getLast().copy().append(", ").append(keyText));
            }

            ++count;
        }

        Component closeThisMessage = Component.translatable("minimap.ui.welcome5").withStyle(ChatFormatting.GRAY);
        addRenderableWidget(closeButton = new PlainTextButton(0, 0, 100, 10, closeThisMessage, button -> {
            options.toggleBooleanValue(EnumOptionsMinimap.WELCOME_SCREEN);
            options.saveAll();

            onClose();
        }, getFont()));

        Component controls = Component.translatable("options.controls").withStyle(ChatFormatting.GRAY);
        addRenderableWidget(controlsButton = new PlainTextButton(0, 0, 100, 10, controls, button -> minecraft.setScreen(new GuiMinimapControls(this)), getFont()));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        int centerX = guiGraphics.guiWidth() / 2;
        int centerY = guiGraphics.guiHeight() / 2;

        int lineHeight = getFont().lineHeight;

        int boxColor = ARGB.color(128, 0, 0, 0);
        int boxTop = centerY - ((welcomeTexts.size() * lineHeight) / 2);

        // Main Box
        int boxWidth = 0;
        for (int i = 1; i < welcomeTexts.size(); ++i) {
            boxWidth = Math.max(boxWidth, getFont().width(welcomeTexts.get(i)));
        }
        drawBox(guiGraphics, centerX - (boxWidth / 2), boxTop, centerX + (boxWidth / 2), boxTop + lineHeight * (welcomeTexts.size() - 1), 4, 1, boxColor);

        // Title Box
        drawBox(guiGraphics, centerX - (boxWidth / 2), boxTop - lineHeight - 3, centerX + (boxWidth / 2), boxTop + lineHeight * (welcomeTexts.size() - 1) + 22, 6, 1, boxColor);

        // Main Text
        for (int i = 1; i < welcomeTexts.size(); ++i) {
            guiGraphics.drawString(getFont(), welcomeTexts.get(i), centerX - (boxWidth / 2), boxTop + lineHeight * (i - 1), 0xFFFFFFFF);
        }

        //  Title Text
        guiGraphics.drawCenteredString(getFont(), welcomeTexts.getFirst(), centerX, boxTop - lineHeight - 3, 0xFFFFFFFF);
        boxTop += lineHeight * welcomeTexts.size();


        // Buttons
        closeButton.setWidth((boxWidth / 2) - 8);
        closeButton.setPosition(getWidth() / 2 + 8, boxTop);
        drawBox(guiGraphics, closeButton.getX(), boxTop, closeButton.getX() + closeButton.getWidth(), boxTop + closeButton.getHeight(), 4, 1, boxColor);

        controlsButton.setWidth((boxWidth / 2) - 8);
        controlsButton.setPosition((getWidth() / 2) - (boxWidth / 2), boxTop);
        drawBox(guiGraphics, controlsButton.getX(), boxTop, controlsButton.getX() + controlsButton.getWidth(), boxTop + controlsButton.getHeight(), 4, 1, boxColor);

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
    public boolean isInGameUi() {
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (keyEvent.key() == GLFW.GLFW_KEY_ESCAPE) {
            options.toggleBooleanValue(EnumOptionsMinimap.WELCOME_SCREEN);
            options.saveAll();
        }

        return super.keyPressed(keyEvent);
    }
}
