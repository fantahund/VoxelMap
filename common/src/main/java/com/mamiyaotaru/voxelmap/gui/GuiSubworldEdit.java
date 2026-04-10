package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.WaypointManager;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;

public class GuiSubworldEdit extends GuiScreenMinimap implements BooleanConsumer {
    private final WaypointManager waypointManager;
    private final ArrayList<?> knownSubworldNames;
    private final String originalSubworldName;
    private String currentSubworldName = "";
    private EditBox subworldNameField;
    private Button doneButton;
    private Button deleteButton;
    private boolean deleteClicked;

    public GuiSubworldEdit(Screen parentGui, String subworldName) {
        super(parentGui, Component.translatable("worldmap.subworld.edit"));

        waypointManager = VoxelConstants.getVoxelMapInstance().getWaypointManager();
        originalSubworldName = subworldName;
        knownSubworldNames = new ArrayList<Object>(waypointManager.getKnownSubworldNames());
    }

    @Override
    public void tick() {
    }

    @Override
    public void init() {
        clearWidgets();

        subworldNameField = new EditBox(getFont(), getWidth() / 2 - 100, getHeight() / 6 + 13, 200, 20, Component.empty());
        subworldNameField.setValue(originalSubworldName);
        setFocused(subworldNameField);

        doneButton = new Button.Builder(Component.translatable("gui.done"), button -> changeNameClicked()).bounds(getWidth() / 2 - 155, getHeight() - 26, 150, 20).build();
        doneButton.active = isNameAcceptable();

        int buttonListY = getHeight() / 6 + 82 + 6;
        deleteButton = new Button.Builder(Component.translatable("selectServer.delete"), button -> deleteClicked()).bounds(getWidth() / 2 - 50, buttonListY + 24, 100, 20).build();
        deleteButton.active = originalSubworldName.equals(subworldNameField.getValue());
        Button cancelButton = new Button.Builder(Component.translatable("gui.cancel"), button -> onClose()).bounds(getWidth() / 2 + 5, getHeight() - 26, 150, 20).build();

        addRenderableWidget(subworldNameField);
        addRenderableWidget(doneButton);
        addRenderableWidget(deleteButton);
        addRenderableWidget(cancelButton);
    }

    @Override
    public void removed() {
    }

    private void changeNameClicked() {
        if (!currentSubworldName.equals(originalSubworldName)) {
            waypointManager.changeSubworldName(originalSubworldName, currentSubworldName);
        }

        onClose();
    }

    private void deleteClicked() {
        deleteClicked = true;

        Component title = Component.translatable("worldmap.subworld.deleteConfirm");
        Component explanation = Component.translatable("selectServer.deleteWarning", originalSubworldName);
        Component affirm = Component.translatable("selectServer.deleteButton");
        Component deny = Component.translatable("gui.cancel");

        minecraft.setScreen(new ConfirmScreen(this, title, explanation, affirm, deny));
    }

    @Override
    public void accept(boolean accept) {
        if (deleteClicked) {
            deleteClicked = false;
            if (accept) {
                waypointManager.deleteSubworld(originalSubworldName);
            }

            onClose();
        }
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        boolean pressed = super.keyPressed(keyEvent);
        boolean acceptable = isNameAcceptable();
        doneButton.active = acceptable;
        deleteButton.active = originalSubworldName.equals(subworldNameField.getValue());
        int keyCode = keyEvent.key();
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && acceptable) {
            changeNameClicked();
        }

        return pressed;
    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        super.render(drawContext, mouseX, mouseY, delta);
        drawContext.drawString(getFont(), Component.translatable("worldmap.subworld.name"), getWidth() / 2 - 100, getHeight() / 6, 0xFFA0A0A0);
    }

    private boolean isNameAcceptable() {
        boolean acceptable;
        currentSubworldName = subworldNameField.getValue();
        acceptable = !currentSubworldName.isEmpty();
        return acceptable && (currentSubworldName.equals(originalSubworldName) || !knownSubworldNames.contains(currentSubworldName));
    }
}
