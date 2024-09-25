package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiButtonText;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiOptionButtonMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

public class GuiMinimapPerformance extends GuiScreenMinimap {
    private static final EnumOptionsMinimap[] relevantOptions = {EnumOptionsMinimap.LIGHTING, EnumOptionsMinimap.TERRAIN, EnumOptionsMinimap.WATERTRANSPARENCY, EnumOptionsMinimap.BLOCKTRANSPARENCY, EnumOptionsMinimap.BIOMES, EnumOptionsMinimap.FILTERING, EnumOptionsMinimap.CHUNKGRID, EnumOptionsMinimap.BIOMEOVERLAY, EnumOptionsMinimap.SLIMECHUNKS, EnumOptionsMinimap.WORLDBORDER};
    private GuiButtonText worldSeedButton;
    private GuiButtonText teleportCommandButton;
    private GuiOptionButtonMinimap slimeChunksButton;
    private final Screen parentScreen;
    protected String screenTitle = "Details / Performance";
    private final MapSettingsManager options;

    public GuiMinimapPerformance(Screen par1GuiScreen) {
        this.parentScreen = par1GuiScreen;
        this.options = VoxelConstants.getVoxelMapInstance().getMapOptions();
    }

    private int getLeftBorder() {
        return this.getWidth() / 2 - 155;
    }

    public void init() {
        this.screenTitle = I18n.get("options.minimap.detailsperformance");
        int leftBorder = this.getLeftBorder();
        int var2 = 0;

        for (EnumOptionsMinimap option : relevantOptions) {
            StringBuilder text = new StringBuilder().append(this.options.getKeyText(option));
            if ((option == EnumOptionsMinimap.WATERTRANSPARENCY || option == EnumOptionsMinimap.BLOCKTRANSPARENCY || option == EnumOptionsMinimap.BIOMES) && !this.options.multicore && this.options.getOptionBooleanValue(option)) {
                text.append("§c").append(text);
            }

            GuiOptionButtonMinimap optionButton = new GuiOptionButtonMinimap(leftBorder + var2 % 2 * 160, this.getHeight() / 6 + 24 * (var2 >> 1), option, Component.literal(text.toString()), this::optionClicked);
            this.addRenderableWidget(optionButton);
            ++var2;
            if (optionButton.returnEnumOptions() == EnumOptionsMinimap.SLIMECHUNKS) {
                this.slimeChunksButton = optionButton;
                this.slimeChunksButton.active = VoxelConstants.getMinecraft().hasSingleplayerServer() || !VoxelConstants.getVoxelMapInstance().getWorldSeed().isEmpty();
            }
        }

        String worldSeedDisplay = VoxelConstants.getVoxelMapInstance().getWorldSeed();
        if (worldSeedDisplay.isEmpty()) {
            worldSeedDisplay = I18n.get("selectWorld.versionUnknown");
        }

        String buttonSeedText = I18n.get("options.minimap.worldseed") + ": " + worldSeedDisplay;
        this.worldSeedButton = new GuiButtonText(this.getFontRenderer(), leftBorder + var2 % 2 * 160, this.getHeight() / 6 + 24 * (var2 >> 1), 150, 20, Component.literal(buttonSeedText), button -> this.worldSeedButton.setEditing(true));
        this.worldSeedButton.setText(VoxelConstants.getVoxelMapInstance().getWorldSeed());
        this.worldSeedButton.active = !VoxelConstants.getMinecraft().hasSingleplayerServer();
        this.addRenderableWidget(this.worldSeedButton);
        ++var2;

        String buttonTeleportText = I18n.get("options.minimap.teleportcommand") + ": " + VoxelConstants.getVoxelMapInstance().getMapOptions().teleportCommand;
        this.teleportCommandButton = new GuiButtonText(this.getFontRenderer(), leftBorder + var2 % 2 * 160, this.getHeight() / 6 + 24 * (var2 >> 1), 150, 20, Component.literal(buttonTeleportText), button -> this.teleportCommandButton.setEditing(true));
        this.teleportCommandButton.setText(VoxelConstants.getVoxelMapInstance().getMapOptions().teleportCommand);
        this.teleportCommandButton.active = VoxelConstants.getVoxelMapInstance().getMapOptions().serverTeleportCommand == null;
        this.addRenderableWidget(this.teleportCommandButton);
        ++var2;
        ++var2;
        this.addRenderableWidget(new Button.Builder(Component.translatable("gui.done"), button -> VoxelConstants.getMinecraft().setScreen(this.parentScreen)).bounds(this.getWidth() / 2 - 100, this.getHeight() / 6 + 168, 200, 20).build());
    }

    @Override
    public void removed() {
    }

    protected void optionClicked(Button par1GuiButton) {
        EnumOptionsMinimap option = ((GuiOptionButtonMinimap) par1GuiButton).returnEnumOptions();
        this.options.setOptionValue(option);
        String perfBomb = "";
        if ((option == EnumOptionsMinimap.WATERTRANSPARENCY || option == EnumOptionsMinimap.BLOCKTRANSPARENCY || option == EnumOptionsMinimap.BIOMES) && !this.options.multicore && this.options.getOptionBooleanValue(option)) {
            perfBomb = "§c";
        }

        par1GuiButton.setMessage(Component.literal(perfBomb + this.options.getKeyText(option)));
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 258) {
            this.worldSeedButton.keyPressed(keyCode, scanCode, modifiers);
            this.teleportCommandButton.keyPressed(keyCode, scanCode, modifiers);
        }

        if ((keyCode == 257 || keyCode == 335)) {
            if (this.worldSeedButton.isEditing()) {
                this.newSeed();
            } else if (this.teleportCommandButton.isEditing()) {
                this.newTeleportCommand();
            }

        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char chr, int modifiers) {
        boolean OK = super.charTyped(chr, modifiers);
        if (chr == '\r') {
            if (this.worldSeedButton.isEditing()) {
                this.newSeed();
            } else if (this.teleportCommandButton.isEditing()) {
                this.newTeleportCommand();
            }

        }

        return OK;
    }

    private void newSeed() {
        String newSeed = this.worldSeedButton.getText();
        VoxelConstants.getVoxelMapInstance().setWorldSeed(newSeed);
        String worldSeedDisplay = VoxelConstants.getVoxelMapInstance().getWorldSeed();
        if (worldSeedDisplay.isEmpty()) {
            worldSeedDisplay = I18n.get("selectWorld.versionUnknown");
        }

        String buttonText = I18n.get("options.minimap.worldseed") + ": " + worldSeedDisplay;
        this.worldSeedButton.setMessage(Component.literal(buttonText));
        this.worldSeedButton.setText(VoxelConstants.getVoxelMapInstance().getWorldSeed());
        VoxelConstants.getVoxelMapInstance().getMap().forceFullRender(true);
        this.slimeChunksButton.active = VoxelConstants.getMinecraft().hasSingleplayerServer() || !VoxelConstants.getVoxelMapInstance().getWorldSeed().isEmpty();
    }

    private void newTeleportCommand() {
        String newTeleportCommand = this.teleportCommandButton.getText().isEmpty() ? "tp %p %x %y %z" : this.teleportCommandButton.getText();
        VoxelConstants.getVoxelMapInstance().getMapOptions().teleportCommand = newTeleportCommand;

        String buttonText = I18n.get("options.minimap.teleportcommand") + ": " + newTeleportCommand;
        this.teleportCommandButton.setMessage(Component.literal(buttonText));
        this.teleportCommandButton.setText(VoxelConstants.getVoxelMapInstance().getMapOptions().teleportCommand);
    }

    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        this.renderTransparentBackground(drawContext);
        drawContext.drawCenteredString(this.getFontRenderer(), this.screenTitle, this.getWidth() / 2, 20, 16777215);
        super.render(drawContext, mouseX, mouseY, delta);
    }
}
