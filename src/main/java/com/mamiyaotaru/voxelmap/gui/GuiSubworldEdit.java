package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.WaypointManager;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.I18n;
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
    private boolean deleteClicked;

    public GuiSubworldEdit(Screen parent, String subworldName) {
        this.parent = parent;
        this.waypointManager = VoxelConstants.getVoxelMapInstance().getWaypointManager();
        this.originalSubworldName = subworldName;
        this.knownSubworldNames = new ArrayList<Object>(this.waypointManager.getKnownSubworldNames());
    }

    public void tick() {
        //this.subworldNameField.setFocused(true);
    }

    public void init() {
        this.clearChildren();
        this.subworldNameField = new TextFieldWidget(this.getFontRenderer(), this.getWidth() / 2 - 100, this.getHeight() / 6 + 13, 200, 20, null);
        this.setFocused(this.subworldNameField);
        this.subworldNameField.setFocused(true);
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

    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        this.renderInGameBackground(drawContext);
        drawContext.drawCenteredTextWithShadow(this.getFontRenderer(), I18n.translate("worldmap.subworld.edit"), this.getWidth() / 2, 20, 16777215);
        drawContext.drawTextWithShadow(this.getFontRenderer(), I18n.translate("worldmap.subworld.name"), this.getWidth() / 2 - 100, this.getHeight() / 6, 10526880);
        this.subworldNameField.render(drawContext, mouseX, mouseY, delta);
        super.render(drawContext, mouseX, mouseY, delta);
    }

    private boolean isNameAcceptable() {
        boolean acceptable;
        this.currentSubworldName = this.subworldNameField.getText();
        acceptable = !this.currentSubworldName.isEmpty();
        return acceptable && (this.currentSubworldName.equals(this.originalSubworldName) || !this.knownSubworldNames.contains(this.currentSubworldName));
    }
}
