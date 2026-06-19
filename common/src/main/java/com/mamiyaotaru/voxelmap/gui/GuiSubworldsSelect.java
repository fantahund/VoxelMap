package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.WaypointManager;
import com.mamiyaotaru.voxelmap.gui.widgets.GuiButtonText;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

public class GuiSubworldsSelect extends GuiScreenMinimap implements BooleanConsumer {
    private Component title;
    private Component subtitle;
    private boolean multiworld;
    private String[] worlds;
    private final WaypointManager waypointManager;
    private final CameraType lastCameraType;

    public GuiSubworldsSelect(Screen parentGui) {
        super(parentGui, Component.empty());
        waypointManager = VoxelConstants.getVoxelMapInstance().getWaypointManager();
        lastCameraType = minecraft.options.getCameraType();
    }

    @Override
    public void init() {
        ArrayList<String> knownSubworldNames = new ArrayList<>(waypointManager.getKnownSubworldNames());
        if (!multiworld && !waypointManager.isMultiworld() && !VoxelConstants.isRealmServer()) {
            ConfirmScreen confirmScreen = new ConfirmScreen(this, Component.translatable("worldmap.multiworld.isThisMultiworld"), Component.translatable("worldmap.multiworld.explanation"), Component.translatable("gui.yes"), Component.translatable("gui.no"));
            minecraft.gui.setScreen(confirmScreen);
        } else {
            minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        }

        clearWidgets();

        title = Component.translatable("worldmap.multiworld.title");
        subtitle = Component.translatable("worldmap.multiworld.select");

        int centerX = getWidth() / 2;
        int buttonsPerRow = Math.max(1, getWidth() / 150);

        int buttonWidth = getWidth() / buttonsPerRow - 5;
        int xSpacing = (getWidth() - buttonsPerRow * buttonWidth) / 2;
        Button cancelBtn = new Button.Builder(Component.translatable("gui.cancel"), button -> VoxelConstants.getMinecraft().gui.setScreen(null)).bounds(centerX - 100, this.height - 30, 200, 20).build();
        addRenderableWidget(cancelBtn);
        knownSubworldNames.sort((name1, name2) -> -String.CASE_INSENSITIVE_ORDER.compare(name1, name2));
        int numKnownSubworlds = knownSubworldNames.size();
        int completeRows = (int) Math.floor((float) (numKnownSubworlds + 1) / buttonsPerRow);
        int lastRowShiftBy = (int) (Math.ceil((float) (numKnownSubworlds + 1) / buttonsPerRow) * buttonsPerRow - (numKnownSubworlds + 1));
        this.worlds = new String[numKnownSubworlds];
        Button[] selectButtons = new Button[numKnownSubworlds + 1];
        Button[] editButtons = new Button[numKnownSubworlds + 1];

        for (int t = 0; t < numKnownSubworlds; ++t) {
            int shiftBy = 1;
            if (t / buttonsPerRow >= completeRows) {
                shiftBy = lastRowShiftBy + 1;
            }

            this.worlds[t] = knownSubworldNames.get(t);
            int tt = t;
            int i = (buttonsPerRow - shiftBy - t % buttonsPerRow) * buttonWidth;
            selectButtons[t] = new Button.Builder(Component.literal(this.worlds[t]), button -> this.worldSelected(this.worlds[tt])).bounds(i + xSpacing, this.height - 60 - t / buttonsPerRow * 21, buttonWidth - 32, 20).build();
            editButtons[t] = new Button.Builder(Component.literal("⚒"), button -> this.editWorld(this.worlds[tt])).bounds(i + xSpacing + buttonWidth - 32, this.height - 60 - t / buttonsPerRow * 21, 30, 20).build();
            this.addRenderableWidget(selectButtons[t]);
            this.addRenderableWidget(editButtons[t]);
        }

        int lastIndex = selectButtons.length - 1;
        int fieldX = (buttonsPerRow - 1 - lastRowShiftBy - lastIndex % buttonsPerRow) * buttonWidth + xSpacing;
        int fieldY = this.height - 60 - lastIndex / buttonsPerRow * 21;
        this.addRenderableWidget(new GuiButtonText(this.getFont(), fieldX, fieldY, buttonWidth - 2, 20, Component.literal("< " + I18n.get("worldmap.multiworld.newName") + " >"), this::nameFieldUpdated));
    }

    @Override
    public void accept(boolean accept) {
        if (accept) {
            multiworld = true;
            minecraft.gui.setScreen(this);
        } else {
            onClose();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int titleWidth = Math.max(getFont().width(title), getFont().width(subtitle));
        graphics.fill(getWidth() / 2 - titleWidth / 2 - 5, 0, getWidth() / 2 + titleWidth / 2 + 5, 27, -1073741824);
        graphics.centeredText(getFont(), title, getWidth() / 2, 5, 0xFFFFFFFF);
        graphics.centeredText(getFont(), subtitle, getWidth() / 2, 15, 0xFFFF0000);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void removed() {
        minecraft.options.setCameraType(lastCameraType);
    }

    private void worldSelected(String selectedSubworldName) {
        waypointManager.setSubworldName(selectedSubworldName, false);
        onClose();
    }

    private void editWorld(String subworldNameToEdit) {
        minecraft.gui.setScreen(new GuiSubworldEdit(this, subworldNameToEdit));
    }

    private void nameFieldUpdated(GuiButtonText field) {
        String newName = field.getText();
        if (!newName.isEmpty()) {
            worldSelected(newName);
        }
    }
}
