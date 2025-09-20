package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.WaypointManager;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.util.ArrayList;
import net.minecraft.client.CameraType;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;

public class GuiSubworldsSelect extends GuiScreenMinimap implements BooleanConsumer {
    private Component title;
    private Component select;
    private boolean multiworld;
    private EditBox newNameField;
    private boolean newWorld;
    private float yaw;
    private final CameraType thirdPersonViewOrig;
    private String[] worlds;
    private final Screen parent;
    final LocalPlayer thePlayer;
    final LocalPlayer camera;
    private final WaypointManager waypointManager;

    public GuiSubworldsSelect(Screen parent) {
        ClientLevel clientWorld = VoxelConstants.getClientWorld();

        this.parent = parent;
        this.thePlayer = VoxelConstants.getPlayer();
        this.camera = new LocalPlayer(VoxelConstants.getMinecraft(), clientWorld, VoxelConstants.getMinecraft().getConnection(), this.thePlayer.getStats(), new ClientRecipeBook(), Input.EMPTY, false);
        this.camera.input = new KeyboardInput(VoxelConstants.getMinecraft().options);
        this.camera.moveOrInterpolateTo(new Vec3(this.thePlayer.getX(), this.thePlayer.getY() + 0.35, this.thePlayer.getZ()), this.thePlayer.getYRot(), 0.0F);
        this.yaw = this.thePlayer.getYRot();
        this.thirdPersonViewOrig = VoxelConstants.getMinecraft().options.getCameraType();
        this.waypointManager = VoxelConstants.getVoxelMapInstance().getWaypointManager();
    }

    @Override
    public void init() {
        ArrayList<String> knownSubworldNames = new ArrayList<>(this.waypointManager.getKnownSubworldNames());
        if (!this.multiworld && !this.waypointManager.isMultiworld() && !VoxelConstants.isRealmServer()) {
            ConfirmScreen confirmScreen = new ConfirmScreen(this, Component.translatable("worldmap.multiworld.isthismultiworld"), Component.translatable("worldmap.multiworld.explanation"), Component.translatable("gui.yes"), Component.translatable("gui.no"));
            VoxelConstants.getMinecraft().setScreen(confirmScreen);
        } else {
            VoxelConstants.getMinecraft().options.setCameraType(CameraType.FIRST_PERSON);
            VoxelConstants.getMinecraft().setCameraEntity(this.camera);
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
            selectButtons[numButtons] = new Button.Builder(Component.literal("< " + I18n.get("worldmap.multiworld.newname") + " >"), button -> {
                this.newWorld = true;
                this.newNameField.setFocused(true);
            }).bounds(i + xSpacing, this.height - 60 - numButtons / buttonsPerRow * 21, buttonWidth - 2, 20).build();
            this.addRenderableWidget(selectButtons[numButtons]);
        }

        this.newNameField = new EditBox(this.getFontRenderer(), i + xSpacing + 1, this.height - 60 - numButtons / buttonsPerRow * 21 + 1, buttonWidth - 4, 18, null);
    }

    @Override
    public void accept(boolean b) {
        if (!b) {
            VoxelConstants.getMinecraft().setScreen(this.parent);
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
            if ((keyCode == 257 || keyCode == 335) && this.newNameField.isFocused()) {
                String newName = this.newNameField.getValue();
                if (newName != null && !newName.isEmpty()) {
                    this.worldSelected(newName);
                }
            }
        }

        return super.keyPressed(keyEvent);
    }

    @Override
    public boolean charTyped(CharacterEvent characterEvent) {
        if (this.newNameField.isFocused()) {
            this.newNameField.charTyped(characterEvent);
            int modifiers = characterEvent.modifiers();
            if (modifiers == 28) {
                String newName = this.newNameField.getValue();
                if (newName != null && !newName.isEmpty()) {
                    this.worldSelected(newName);
                }
            }
        }

        return super.charTyped(characterEvent);
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        int titleStringWidth = this.getFontRenderer().width(this.title);
        titleStringWidth = Math.max(titleStringWidth, this.getFontRenderer().width(this.select));
        drawContext.fill(this.width / 2 - titleStringWidth / 2 - 5, 0, this.width / 2 + titleStringWidth / 2 + 5, 27, -1073741824);
        drawContext.drawCenteredString(this.getFontRenderer(), this.title, this.width / 2, 5, 0xFFFFFFFF);
        drawContext.drawCenteredString(this.getFontRenderer(), this.select, this.width / 2, 15, 0xFFFF0000);
        this.camera.xRotO = 0.0F;
        this.camera.setXRot(0.0F);
        this.camera.yRotO = this.yaw;
        this.camera.setYRot(this.yaw);
        float var4 = 0.475F;
        this.camera.yOld = this.camera.yo = this.thePlayer.getY();
        this.camera.xOld = this.camera.xo = this.thePlayer.getX() - var4 * Math.sin(this.yaw / 180.0 * Math.PI);
        this.camera.zOld = this.camera.zo = this.thePlayer.getZ() + var4 * Math.cos(this.yaw / 180.0 * Math.PI);
        this.camera.setPosRaw(this.camera.xo, this.camera.yo, this.camera.zo);
        float var5 = 1.0F;
        this.yaw = (float) (this.yaw + var5 * (1.0 + 0.7F * Math.cos((this.yaw + 45.0F) / 45.0 * Math.PI)));
        super.render(drawContext, mouseX, mouseY, delta);
        if (this.newWorld) {
            this.newNameField.render(drawContext, mouseX, mouseY, delta);
        }

    }

    @Override
    public void removed() {
        super.removed();
        VoxelConstants.getMinecraft().options.setCameraType(this.thirdPersonViewOrig);
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
