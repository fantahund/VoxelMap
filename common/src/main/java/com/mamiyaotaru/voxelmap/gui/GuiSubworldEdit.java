package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.WaypointManager;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.util.ArrayList;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class GuiSubworldEdit extends GuiScreenMinimap implements BooleanConsumer {
    private final Screen parent;
    private final WaypointManager waypointManager;
    private final ArrayList<?> knownSubworldNames;
    private final String originalSubworldName;
    private String currentSubworldName = "";
    private EditBox subworldNameField;
    private Button doneButton;
    private Button deleteButton;
    private boolean deleteClicked;

    public GuiSubworldEdit(Screen parent, String subworldName) {
        this.parent = parent;
        this.setParentScreen(this.parent);

        this.waypointManager = VoxelConstants.getVoxelMapInstance().getWaypointManager();
        this.originalSubworldName = subworldName;
        this.knownSubworldNames = new ArrayList<Object>(this.waypointManager.getKnownSubworldNames());
    }

    @Override
    public void tick() {
    }

    @Override
    public void init() {
        this.clearWidgets();
        this.subworldNameField = new EditBox(this.getFont(), this.getWidth() / 2 - 100, this.getHeight() / 6 + 13, 200, 20, null);
        this.setFocused(this.subworldNameField);
        this.subworldNameField.setFocused(true);
        this.subworldNameField.setValue(this.originalSubworldName);
        this.addRenderableWidget(this.subworldNameField);
        this.addRenderableWidget(this.doneButton = new Button.Builder(Component.translatable("gui.done"), button -> this.changeNameClicked()).bounds(this.getWidth() / 2 - 155, this.getHeight() - 28, 150, 20).build());
        this.addRenderableWidget(new Button.Builder(Component.translatable("gui.cancel"), button -> VoxelConstants.getMinecraft().setScreen(this.parent)).bounds(this.getWidth() / 2 + 5, this.getHeight() - 28, 150, 20).build());
        int buttonListY = this.getHeight() / 6 + 82 + 6;
        this.addRenderableWidget(this.deleteButton = new Button.Builder(Component.translatable("selectServer.delete"), button -> this.deleteClicked()).bounds(this.getWidth() / 2 - 50, buttonListY + 24, 100, 20).build());
        this.doneButton.active = this.isNameAcceptable();
        this.deleteButton.active = this.originalSubworldName.equals(this.subworldNameField.getValue());
    }

    @Override
    public void removed() {
    }

    private void changeNameClicked() {
        if (!this.currentSubworldName.equals(this.originalSubworldName)) {
            this.waypointManager.changeSubworldName(this.originalSubworldName, this.currentSubworldName);
        }

        VoxelConstants.getMinecraft().setScreen(this.parent);
    }

    private void deleteClicked() {
        this.deleteClicked = true;
        Component title = Component.translatable("worldmap.subworld.deleteconfirm");
        Component explanation = Component.translatable("selectServer.deleteWarning", this.originalSubworldName);
        Component affirm = Component.translatable("selectServer.deleteButton");
        Component deny = Component.translatable("gui.cancel");
        ConfirmScreen confirmScreen = new ConfirmScreen(this, title, explanation, affirm, deny);
        VoxelConstants.getMinecraft().setScreen(confirmScreen);
    }

    @Override
    public void accept(boolean b) {
        if (this.deleteClicked) {
            this.deleteClicked = false;
            if (b) {
                this.waypointManager.deleteSubworld(this.originalSubworldName);
            }

            VoxelConstants.getMinecraft().setScreen(this.parent);
        }

    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        int keyCode = keyEvent.key();
        boolean OK = super.keyPressed(keyEvent);
        boolean acceptable = this.isNameAcceptable();
        this.doneButton.active = this.isNameAcceptable();
        this.deleteButton.active = this.originalSubworldName.equals(this.subworldNameField.getValue());
        if ((keyCode == 257 || keyCode == 335) && acceptable) {
            this.changeNameClicked();
        }

        return OK;
    }

    @Override
    public boolean charTyped(CharacterEvent characterEvent) {
        boolean OK = super.charTyped(characterEvent);
        boolean acceptable = this.isNameAcceptable();
        this.doneButton.active = this.isNameAcceptable();
        this.deleteButton.active = this.originalSubworldName.equals(this.subworldNameField.getValue());
        if (characterEvent.codepoint() == '\r' && acceptable) {
            this.changeNameClicked();
        }

        return OK;
    }


    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
        this.subworldNameField.mouseClicked(mouseButtonEvent, doubleClick);
        return super.mouseClicked(mouseButtonEvent, doubleClick);
    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        drawContext.drawCenteredString(this.getFont(), Component.translatable("worldmap.subworld.edit"), this.getWidth() / 2, 20, 0xFFFFFFFF);
        drawContext.drawString(this.getFont(), Component.translatable("worldmap.subworld.name"), this.getWidth() / 2 - 100, this.getHeight() / 6, 0xFFA0A0A0);
        this.subworldNameField.render(drawContext, mouseX, mouseY, delta);
        super.render(drawContext, mouseX, mouseY, delta);
    }

    private boolean isNameAcceptable() {
        boolean acceptable;
        this.currentSubworldName = this.subworldNameField.getValue();
        acceptable = !this.currentSubworldName.isEmpty();
        return acceptable && (this.currentSubworldName.equals(this.originalSubworldName) || !this.knownSubworldNames.contains(this.currentSubworldName));
    }
}
