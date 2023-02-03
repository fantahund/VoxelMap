package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.WaypointManager;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Optional;

public class GuiSubworldsSelect extends GuiScreenMinimap implements BooleanConsumer {
    private Text title;
    private Text select;
    private boolean multiworld;
    private TextFieldWidget newNameField;
    private boolean newWorld;
    private float yaw;
    private final Perspective thirdPersonViewOrig;
    private String[] worlds;
    private final Screen parent;
    final ClientPlayerEntity thePlayer;
    final ClientPlayerEntity camera;
    private final WaypointManager waypointManager;

    public GuiSubworldsSelect(Screen parent) {
        Optional<ClientWorld> optionalClientWorld = VoxelConstants.getClientWorld();

        if (optionalClientWorld.isEmpty()) {
            String error = "ClientWorld not present while expected to be!";

            VoxelConstants.getLogger().fatal(error);
            throw new IllegalStateException(error);
        }

        ClientWorld clientWorld = optionalClientWorld.get();

        this.parent = parent;
        this.thePlayer = VoxelConstants.getPlayer();
        this.camera = new ClientPlayerEntity(VoxelConstants.getMinecraft(), clientWorld, VoxelConstants.getMinecraft().getNetworkHandler(), this.thePlayer.getStatHandler(), new ClientRecipeBook(), false, false);
        this.camera.input = new KeyboardInput(VoxelConstants.getMinecraft().options);
        this.camera.refreshPositionAndAngles(this.thePlayer.getX(), this.thePlayer.getY() - this.thePlayer.getHeightOffset(), this.thePlayer.getZ(), this.thePlayer.getYaw(), 0.0F);
        this.yaw = this.thePlayer.getYaw();
        this.thirdPersonViewOrig = VoxelConstants.getMinecraft().options.getPerspective();
        this.waypointManager = VoxelConstants.getVoxelMapInstance().getWaypointManager();
    }

    public void init() {
        ArrayList<String> knownSubworldNames = new ArrayList<>(this.waypointManager.getKnownSubworldNames());
        if (!this.multiworld && !this.waypointManager.isMultiworld() && !VoxelConstants.getMinecraft().isConnectedToRealms()) {
            ConfirmScreen confirmScreen = new ConfirmScreen(this, Text.translatable("worldmap.multiworld.isthismultiworld"), Text.translatable("worldmap.multiworld.explanation"), Text.translatable("gui.yes"), Text.translatable("gui.no"));
            VoxelConstants.getMinecraft().setScreen(confirmScreen);
        } else {
            VoxelConstants.getMinecraft().options.setPerspective(Perspective.FIRST_PERSON);
            VoxelConstants.getMinecraft().setCameraEntity(this.camera);
        }

        this.title = Text.translatable("worldmap.multiworld.title");
        this.select = Text.translatable("worldmap.multiworld.select");
        this.clearChildren();
        int centerX = this.width / 2;
        int buttonsPerRow = this.width / 150;
        if (buttonsPerRow == 0) {
            buttonsPerRow = 1;
        }

        int buttonWidth = this.width / buttonsPerRow - 5;
        int xSpacing = (this.width - buttonsPerRow * buttonWidth) / 2;
        ButtonWidget cancelBtn = new ButtonWidget.Builder(Text.translatable("gui.cancel"), button -> VoxelConstants.getMinecraft().setScreen(null)).dimensions(centerX - 100, this.height - 30, 200, 20).build();
        this.addDrawableChild(cancelBtn);
        knownSubworldNames.sort((name1, name2) -> -String.CASE_INSENSITIVE_ORDER.compare(name1, name2));
        int numKnownSubworlds = knownSubworldNames.size();
        int completeRows = (int) Math.floor((float) (numKnownSubworlds + 1) / buttonsPerRow);
        int lastRowShiftBy = (int) (Math.ceil((float) (numKnownSubworlds + 1) / buttonsPerRow) * buttonsPerRow - (numKnownSubworlds + 1));
        this.worlds = new String[numKnownSubworlds];
        ButtonWidget[] selectButtons = new ButtonWidget[numKnownSubworlds + 1];
        ButtonWidget[] editButtons = new ButtonWidget[numKnownSubworlds + 1];

        for (int t = 0; t < numKnownSubworlds; ++t) {
            int shiftBy = 1;
            if (t / buttonsPerRow >= completeRows) {
                shiftBy = lastRowShiftBy + 1;
            }

            this.worlds[t] = knownSubworldNames.get(t);
            int tt = t;
            int i = (buttonsPerRow - shiftBy - t % buttonsPerRow) * buttonWidth;
            selectButtons[t] = new ButtonWidget.Builder(Text.literal(this.worlds[t]), button -> this.worldSelected(this.worlds[tt])).dimensions(i + xSpacing, this.height - 60 - t / buttonsPerRow * 21, buttonWidth - 32, 20).build();
            editButtons[t] = new ButtonWidget.Builder(Text.literal("⚒"), button -> this.editWorld(this.worlds[tt])).dimensions(i + xSpacing + buttonWidth - 32, this.height - 60 - t / buttonsPerRow * 21, 30, 20).build();
            this.addDrawableChild(selectButtons[t]);
            this.addDrawableChild(editButtons[t]);
        }

        int numButtons = selectButtons.length - 1;
        int i = (buttonsPerRow - 1 - lastRowShiftBy - numButtons % buttonsPerRow) * buttonWidth;
        if (!this.newWorld) {
            selectButtons[numButtons] = new ButtonWidget.Builder(Text.literal("< " + I18n.translate("worldmap.multiworld.newname") + " >"), button -> {
                this.newWorld = true;
                this.newNameField.setFocused(true);
            }).dimensions(i + xSpacing, this.height - 60 - numButtons / buttonsPerRow * 21, buttonWidth - 2, 20).build();
            this.addDrawableChild(selectButtons[numButtons]);
        }

        this.newNameField = new TextFieldWidget(this.getFontRenderer(), i + xSpacing + 1, this.height - 60 - numButtons / buttonsPerRow * 21 + 1, buttonWidth - 4, 18, null);
    }

    public void accept(boolean b) {
        if (!b) {
            VoxelConstants.getMinecraft().setScreen(this.parent);
        } else {
            this.multiworld = true;
            VoxelConstants.getMinecraft().setScreen(this);
        }

    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.newWorld) {
            this.newNameField.mouseClicked(mouseX, mouseY, button);
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.newNameField.isFocused()) {
            this.newNameField.keyPressed(keyCode, scanCode, modifiers);
            if ((keyCode == 257 || keyCode == 335) && this.newNameField.isFocused()) {
                String newName = this.newNameField.getText();
                if (newName != null && !newName.isEmpty()) {
                    this.worldSelected(newName);
                }
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char chr, int modifiers) {
        if (this.newNameField.isFocused()) {
            this.newNameField.charTyped(chr, modifiers);
            if (modifiers == 28) {
                String newName = this.newNameField.getText();
                if (newName != null && !newName.isEmpty()) {
                    this.worldSelected(newName);
                }
            }
        }

        return super.charTyped(chr, modifiers);
    }

    public void tick() {
        this.newNameField.tick();
        super.tick();
    }

    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        int titleStringWidth = this.getFontRenderer().getWidth(this.title);
        titleStringWidth = Math.max(titleStringWidth, this.getFontRenderer().getWidth(this.select));
        fill(matrices, this.width / 2 - titleStringWidth / 2 - 5, 0, this.width / 2 + titleStringWidth / 2 + 5, 27, -1073741824);
        drawCenteredText(matrices, this.getFontRenderer(), this.title, this.width / 2, 5, 16777215);
        drawCenteredText(matrices, this.getFontRenderer(), this.select, this.width / 2, 15, 16711680);
        this.camera.prevPitch = 0.0F;
        this.camera.setPitch(0.0F);
        this.camera.prevYaw = this.yaw;
        this.camera.setYaw(this.yaw);
        float var4 = 0.475F;
        this.camera.lastRenderY = this.camera.prevY = this.thePlayer.getY();
        this.camera.lastRenderX = this.camera.prevX = this.thePlayer.getX() - var4 * Math.sin(this.yaw / 180.0 * Math.PI);
        this.camera.lastRenderZ = this.camera.prevZ = this.thePlayer.getZ() + var4 * Math.cos(this.yaw / 180.0 * Math.PI);
        this.camera.setPos(this.camera.prevX, this.camera.prevY, this.camera.prevZ);
        float var5 = 1.0F;
        this.yaw = (float) (this.yaw + var5 * (1.0 + 0.7F * Math.cos((this.yaw + 45.0F) / 45.0 * Math.PI)));
        super.render(matrices, mouseX, mouseY, delta);
        if (this.newWorld) {
            this.newNameField.render(matrices, mouseX, mouseY, delta);
        }

    }

    @Override
    public void removed() {
        super.removed();
        VoxelConstants.getMinecraft().options.setPerspective(this.thirdPersonViewOrig);
        VoxelConstants.getMinecraft().setCameraEntity(this.thePlayer);
    }

    private void worldSelected(String selectedSubworldName) {
        this.waypointManager.setSubworldName(selectedSubworldName, false);
        VoxelConstants.getMinecraft().setScreen(this.parent);
    }

    private void editWorld(String subworldNameToEdit) {
        VoxelConstants.getMinecraft().setScreen(new GuiSubworldEdit(this, subworldNameToEdit));
    }
}
