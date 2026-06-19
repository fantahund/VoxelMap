package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.options.containers.MapOptions;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

import java.util.ArrayList;
import java.util.Arrays;

public class GuiWelcomeScreen extends GuiScreenMinimap {
    private final MapOptions options;
    private final ArrayList<StringWidget> welcomeTexts = new ArrayList<>();
    private int lineStart = 1;
    private PlainTextButton scrollUpButton;
    private PlainTextButton scrollDownButton;
    private PlainTextButton closeButton;
    private PlainTextButton controlsButton;

    public GuiWelcomeScreen(Screen parentGui) {
        super(parentGui, Component.empty());
        options = VoxelConstants.getVoxelMapInstance().getMapOptions();
    }

    @Override
    public void init() {
        clearWidgets();

        Component maintainers = Component.literal("The VoxelMap Team").withStyle((style) -> style
                .withColor(ChatFormatting.GREEN)
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("fantahund\nBrokkonaut\nTheAlgorithm476\nAlgamja11")))
        );
        welcomeTexts.clear();
        welcomeTexts.add(createString(Component.literal("VoxelMap!").withStyle(ChatFormatting.RED)));
        welcomeTexts.add(createString(Component.translatable("minimap.ui.welcome1", maintainers)));
        welcomeTexts.add(createString(Component.translatable("minimap.ui.welcome2")));
        welcomeTexts.add(createString(Component.empty()));
        welcomeTexts.add(createString(Component.translatable("minimap.ui.welcome3")));
        welcomeTexts.add(createString(Component.translatable("minimap.ui.welcome4")));
        welcomeTexts.add(createString(Component.empty()));
        KeyMapping[] sortedKeys = Arrays.stream(options.keyBindings).sorted().toArray(KeyMapping[]::new);
        for (KeyMapping key : sortedKeys) {
            if (key.isUnbound()) {
                continue;
            }
            Component keyText = Component.translatable("options.generic_value", key.getTranslatedKeyMessage().copy().withStyle(ChatFormatting.AQUA), Component.translatable(key.getName()));
            welcomeTexts.add(createString(keyText));
        }

        addRenderableWidget(scrollUpButton = new PlainTextButton(0, 0, 10, 10, Component.literal("↑"), button -> scrollLine(1.0), getFont()));
        addRenderableWidget(scrollDownButton = new PlainTextButton(0, 0, 10, 10, Component.literal("↓"), button -> scrollLine(-1.0), getFont()));

        Component closeMessage = Component.translatable("minimap.ui.welcome5").withStyle(ChatFormatting.GRAY);
        addRenderableWidget(closeButton = new PlainTextButton(0, 0, 0, 0, closeMessage, button -> onClose(), getFont()));

        Component controlsMessage = Component.translatable("options.controls").withStyle(ChatFormatting.GRAY);
        addRenderableWidget(controlsButton = new PlainTextButton(0, 0, 0, 0, controlsMessage, button -> minecraft.setScreen(new GuiMinimapControls(this)), getFont()));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double amount) {
        scrollLine(amount);
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, amount);
    }

    private void scrollLine(double amount) {
        if (amount > 0.0) {
            lineStart--;
        } else if (amount < 0.0) {
            lineStart++;
        }
        lineStart = Math.min(Math.max(lineStart, 1), Math.max(welcomeTexts.size() - 9, 1));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int lineHeight = getFont().lineHeight;
        int width = 0;
        for (StringWidget widget : welcomeTexts) {
            width = Math.max(width, widget.getWidth());
            removeWidget(widget);
        }
        int height = lineHeight * 9;
        int top = (getHeight() - height) / 2;
        int bottom = (getHeight() + height) / 2;
        int left = (getWidth() - width) / 2;
        int right = (getWidth() + width) / 2;

        graphics.fill(left - 6, top - lineHeight - 4, right + 6, bottom + 20, 0x80000000);
        graphics.fill(left - 4, top - 1, right + 4, bottom + 1, 0x80000000);

        welcomeTexts.getFirst().setPosition((getWidth() - welcomeTexts.getFirst().getWidth()) / 2, top - lineHeight - 2);
        addRenderableWidget(welcomeTexts.getFirst());

        int lineEnd = Math.min(lineStart + 9, welcomeTexts.size());
        for (int i = lineStart; i < lineEnd; i++) {
            welcomeTexts.get(i).setPosition((getWidth() - width) / 2, top + lineHeight * (i - lineStart));
            addRenderableWidget(welcomeTexts.get(i));
        }

        scrollUpButton.setPosition(right - 4, top + 2);
        scrollDownButton.setPosition(right - 4, bottom - 12);

        int buttonWidth = width / 2 - 8;

        closeButton.setRectangle(buttonWidth, 10, getWidth() / 2 + 8, bottom + 6);
        drawButtonBackground(graphics, closeButton);

        controlsButton.setRectangle(buttonWidth, 10, (getWidth() - width) / 2, bottom + 6);
        drawButtonBackground(graphics, controlsButton);

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private StringWidget createString(Component component) {
        return new StringWidget(component, minecraft.font);
    }

    private void drawButtonBackground(GuiGraphicsExtractor graphics, AbstractWidget widget) {
        graphics.fill(widget.getX() - 4, widget.getY() - 1, widget.getX() + widget.getWidth() + 4, widget.getY() + widget.getHeight() + 1, 0x80000000);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
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
    public void onClose() {
        super.onClose();
        options.welcome.set(false);
    }
}
