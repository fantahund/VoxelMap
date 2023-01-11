package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.VoxelMap;
import com.mamiyaotaru.voxelmap.WaypointManager;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import com.mamiyaotaru.voxelmap.util.I18nUtils;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.util.ArrayList;

public class GuiSubworldEdit extends GuiScreenMinimap implements BooleanConsumer {
    private final Screen parent;
    private final WaypointManager waypointManager;
    private final ArrayList<?> knownSubworldNames;
    private final String originalSubworldName;
    private String currentSubworldName = "";
    private TextFieldWidget subworldNameField;
    private ButtonWidget doneButton;
    private ButtonWidget deleteButton;
    private boolean deleteClicked = false;

    public GuiSubworldEdit(Screen parent, VoxelMap master, String subworldName) {
        this.parent = parent;
        this.waypointManager = master.getWaypointManager();
        this.originalSubworldName = subworldName;
        this.knownSubworldNames = new ArrayList<Object>(this.waypointManager.getKnownSubworldNames());
    }

    public void tick() {
        this.subworldNameField.tick();
    }

    public void init() {
        this.clearChildren();
        this.subworldNameField = new TextFieldWidget(this.getFontRenderer(), this.getWidth() / 2 - 100, this.getHeight() / 6 + 13, 200, 20, null);
        this.setFocused(this.subworldNameField);
        this.subworldNameField.setTextFieldFocused(true);
        this.subworldNameField.setText(this.originalSubworldName);
        this.addDrawableChild(this.subworldNameField);
        this.addDrawableChild(this.doneButton = new ButtonWidget.Builder(Text.translatable("gui.done"), button -> this.changeNameClicked()).dimensions(this.getWidth() / 2 - 155, this.getHeight() / 6 + 168, 150, 20).build());
        this.addDrawableChild(new ButtonWidget.Builder(Text.translatable("gui.cancel"), button -> VoxelConstants.getMinecraft().setScreen(this.parent)).dimensions(this.getWidth() / 2 + 5, this.getHeight() / 6 + 168, 150, 20).build());
        int buttonListY = this.getHeight() / 6 + 82 + 6;
        this.addDrawableChild(this.deleteButton = new ButtonWidget.Builder(Text.translatable("selectServer.delete"), button -> this.deleteClicked()).dimensions(this.getWidth() / 2 - 50, buttonListY + 24, 100, 20).build());
        this.doneButton.active = this.isNameAcceptable();
        this.deleteButton.active = this.originalSubworldName.equals(this.subworldNameField.getText());
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
        Text title = Text.translatable("worldmap.subworld.deleteconfirm");
        Text explanation = Text.translatable("selectServer.deleteWarning", this.originalSubworldName);
        Text affirm = Text.translatable("selectServer.deleteButton");
        Text deny = Text.translatable("gui.cancel");
        ConfirmScreen confirmScreen = new ConfirmScreen(this, title, explanation, affirm, deny);
        VoxelConstants.getMinecraft().setScreen(confirmScreen);
    }

    public void accept(boolean b) {
        if (this.deleteClicked) {
            this.deleteClicked = false;
            if (b) {
                this.waypointManager.deleteSubworld(this.originalSubworldName);
            }

            VoxelConstants.getMinecraft().setScreen(this.parent);
        }

    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean OK = super.keyPressed(keyCode, scanCode, modifiers);
        boolean acceptable = this.isNameAcceptable();
        this.doneButton.active = this.isNameAcceptable();
        this.deleteButton.active = this.originalSubworldName.equals(this.subworldNameField.getText());
        if ((keyCode == 257 || keyCode == 335) && acceptable) {
            this.changeNameClicked();
        }

        return OK;
    }

    public boolean charTyped(char chr, int modifiers) {
        boolean OK = super.charTyped(chr, modifiers);
        boolean acceptable = this.isNameAcceptable();
        this.doneButton.active = this.isNameAcceptable();
        this.deleteButton.active = this.originalSubworldName.equals(this.subworldNameField.getText());
        if (chr == '\r' && acceptable) {
            this.changeNameClicked();
        }

        return OK;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.subworldNameField.mouseClicked(mouseX, mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.drawMap(matrices);
        this.renderBackground(matrices);
        drawCenteredText(matrices, this.getFontRenderer(), I18nUtils.getString("worldmap.subworld.edit"), this.getWidth() / 2, 20, 16777215);
        drawStringWithShadow(matrices, this.getFontRenderer(), I18nUtils.getString("worldmap.subworld.name"), this.getWidth() / 2 - 100, this.getHeight() / 6, 10526880);
        this.subworldNameField.render(matrices, mouseX, mouseY, delta);
        super.render(matrices, mouseX, mouseY, delta);
    }

    private boolean isNameAcceptable() {
        boolean acceptable;
        this.currentSubworldName = this.subworldNameField.getText();
        acceptable = this.currentSubworldName.length() > 0;
        return acceptable && (this.currentSubworldName.equals(this.originalSubworldName) || !this.knownSubworldNames.contains(this.currentSubworldName));
    }
}
