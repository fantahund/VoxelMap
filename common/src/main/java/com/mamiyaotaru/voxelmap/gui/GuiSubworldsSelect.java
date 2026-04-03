package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.WaypointManager;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;

public class GuiSubworldsSelect extends GuiScreenMinimap implements BooleanConsumer {
    private Component title;
    private Component select;
    private boolean multiworld;
    private EditBox newNameField;
    private boolean newWorld;
    private String[] worlds;
    private final WaypointManager waypointManager;
    private final CameraType lastCameraType;

    public GuiSubworldsSelect(Screen parent) {
        lastScreen = parent;
        waypointManager = VoxelConstants.getVoxelMapInstance().getWaypointManager();
        lastCameraType = VoxelConstants.getMinecraft().options.getCameraType();
    }

    @Override
    public void init() {
        ArrayList<String> knownSubworldNames = new ArrayList<>(this.waypointManager.getKnownSubworldNames());
        if (!this.multiworld && !this.waypointManager.isMultiworld() && !VoxelConstants.isRealmServer()) {
            ConfirmScreen confirmScreen = new ConfirmScreen(this, Component.translatable("worldmap.multiworld.isThisMultiworld"), Component.translatable("worldmap.multiworld.explanation"), Component.translatable("gui.yes"), Component.translatable("gui.no"));
            VoxelConstants.getMinecraft().setScreen(confirmScreen);
        } else {
            VoxelConstants.getMinecraft().options.setCameraType(CameraType.FIRST_PERSON);
        }

        this.title = Component.translatable("worldmap.multiworld.title");
        this.select = Component.translatable("worldmap.multiworld.select");
        this.clearWidgets();
        int centerX = this.width / 2;
        int buttonsPerRow = this.width / 150;
        if (buttonsPerRow == 0) {
            buttonsPerRow = 1;
        }

        int buttonWidth = this.width / buttonsPerRow - 5;
        int xSpacing = (this.width - buttonsPerRow * buttonWidth) / 2;
        Button cancelBtn = new Button.Builder(Component.translatable("gui.cancel"), button -> VoxelConstants.getMinecraft().setScreen(null)).bounds(centerX - 100, this.height - 30, 200, 20).build();
        this.addRenderableWidget(cancelBtn);
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

        int numButtons = selectButtons.length - 1;
        int i = (buttonsPerRow - 1 - lastRowShiftBy - numButtons % buttonsPerRow) * buttonWidth;
        if (!this.newWorld) {
            selectButtons[numButtons] = new Button.Builder(Component.literal("< " + I18n.get("worldmap.multiworld.newName") + " >"), button -> {
                this.newWorld = true;
                this.newNameField.setFocused(true);
            }).bounds(i + xSpacing, this.height - 60 - numButtons / buttonsPerRow * 21, buttonWidth - 2, 20).build();
            this.addRenderableWidget(selectButtons[numButtons]);
        }

        this.newNameField = new EditBox(this.getFont(), i + xSpacing + 1, this.height - 60 - numButtons / buttonsPerRow * 21 + 1, buttonWidth - 4, 18, Component.empty());
    }

    @Override
    public void accept(boolean b) {
        if (!b) {
            this.onClose();
        } else {
            this.multiworld = true;
            VoxelConstants.getMinecraft().setScreen(this);
        }

    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean bl) {
        if (this.newWorld) {
            this.newNameField.mouseClicked(mouseButtonEvent, bl);
        }

        return super.mouseClicked(mouseButtonEvent, bl);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (this.newNameField.isFocused()) {
            this.newNameField.keyPressed(keyEvent);
            int keyCode = keyEvent.key(); //TODO 1.21.9
            if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && this.newNameField.isFocused()) {
                String newName = this.newNameField.getValue();
                if (newName != null && !newName.isEmpty()) {
                    this.worldSelected(newName);
                }
            }
        }

        return super.keyPressed(keyEvent);
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int titleStringWidth = this.getFont().width(this.title);
        titleStringWidth = Math.max(titleStringWidth, this.getFont().width(this.select));
        graphics.fill(this.width / 2 - titleStringWidth / 2 - 5, 0, this.width / 2 + titleStringWidth / 2 + 5, 27, -1073741824);
        graphics.text(this.getFont(), this.title, this.width / 2, 5, 0xFFFFFFFF);
        graphics.text(this.getFont(), this.select, this.width / 2, 15, 0xFFFF0000);

        super.extractRenderState(graphics, mouseX, mouseY, delta);
        if (this.newWorld) {
            this.newNameField.extractRenderState(graphics, mouseX, mouseY, delta);
        }

    }

    @Override
    public void extractMenuBackground(GuiGraphicsExtractor graphics) {
    }

    @Override
    public void extractBlurredBackground(GuiGraphicsExtractor graphics) {
    }

    @Override
    public void removed() {
        VoxelConstants.getMinecraft().options.setCameraType(lastCameraType);
    }

    private void worldSelected(String selectedSubworldName) {
        this.waypointManager.setSubworldName(selectedSubworldName, false);
        this.onClose();
    }

    private void editWorld(String subworldNameToEdit) {
        VoxelConstants.getMinecraft().setScreen(new GuiSubworldEdit(this, subworldNameToEdit));
    }
}
