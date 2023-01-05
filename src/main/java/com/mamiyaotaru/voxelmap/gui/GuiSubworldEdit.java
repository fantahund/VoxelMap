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

    public void accept(boolean par1) {
        if (this.deleteClicked) {
            this.deleteClicked = false;
            if (par1) {
                this.waypointManager.deleteSubworld(this.originalSubworldName);
            }

            VoxelConstants.getMinecraft().setScreen(this.parent);
        }

    }

    public boolean keyPressed(int keysm, int scancode, int b) {
        boolean OK = super.keyPressed(keysm, scancode, b);
        boolean acceptable = this.isNameAcceptable();
        this.doneButton.active = this.isNameAcceptable();
        this.deleteButton.active = this.originalSubworldName.equals(this.subworldNameField.getText());
        if ((keysm == 257 || keysm == 335) && acceptable) {
            this.changeNameClicked();
        }

        return OK;
    }

    public boolean charTyped(char character, int keycode) {
        boolean OK = super.charTyped(character, keycode);
        boolean acceptable = this.isNameAcceptable();
        this.doneButton.active = this.isNameAcceptable();
        this.deleteButton.active = this.originalSubworldName.equals(this.subworldNameField.getText());
        if (character == '\r' && acceptable) {
            this.changeNameClicked();
        }

        return OK;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int par3) {
        this.subworldNameField.mouseClicked(mouseX, mouseY, par3);
        return super.mouseClicked(mouseX, mouseY, par3);
    }

    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.drawMap(matrixStack);
        this.renderBackground(matrixStack);
        drawCenteredText(matrixStack, this.getFontRenderer(), I18nUtils.getString("worldmap.subworld.edit"), this.getWidth() / 2, 20, 16777215);
        drawStringWithShadow(matrixStack, this.getFontRenderer(), I18nUtils.getString("worldmap.subworld.name"), this.getWidth() / 2 - 100, this.getHeight() / 6, 10526880);
        this.subworldNameField.render(matrixStack, mouseX, mouseY, partialTicks);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    private boolean isNameAcceptable() {
        boolean acceptable;
        this.currentSubworldName = this.subworldNameField.getText();
        acceptable = this.currentSubworldName.length() > 0;
        return acceptable && (this.currentSubworldName.equals(this.originalSubworldName) || !this.knownSubworldNames.contains(this.currentSubworldName));
    }
}
