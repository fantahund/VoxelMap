package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.VoxelMap;
import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiOptionButtonMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import com.mamiyaotaru.voxelmap.persistent.GuiPersistentMapOptions;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

public class GuiMinimapOptions extends GuiScreenMinimap {
    private final Screen parent;
    private final MapSettingsManager options;
    protected String screenTitle = "Minimap Options";

    public GuiMinimapOptions(Screen parent) {
        this.parent = parent;
        this.options = VoxelConstants.getVoxelMapInstance().getMapOptions();
    }

    public void init() {
        EnumOptionsMinimap[] relevantOptions = { EnumOptionsMinimap.COORDS, EnumOptionsMinimap.HIDE, EnumOptionsMinimap.LOCATION, EnumOptionsMinimap.SIZE, EnumOptionsMinimap.SQUARE, EnumOptionsMinimap.ROTATES, EnumOptionsMinimap.BEACONS, EnumOptionsMinimap.CAVEMODE, EnumOptionsMinimap.MOVEMAPDOWNWHILESTATSUEFFECT, EnumOptionsMinimap.MOVESCOREBOARDDOWN };
        this.screenTitle = I18n.get("options.minimap.title");

        for (int i = 0; i < relevantOptions.length; i++) {
            EnumOptionsMinimap option = relevantOptions[i];
            GuiOptionButtonMinimap optionButton = new GuiOptionButtonMinimap(getWidth() / 2 - 155 + i % 2 * 160, getHeight() / 6 + 24 * (i >> 1), option, Component.literal(options.getKeyText(option)), this::optionClicked);
            this.addRenderableWidget(optionButton);

            if (option == EnumOptionsMinimap.HIDE) optionButton.active = this.options.minimapAllowed;
            if (option == EnumOptionsMinimap.BEACONS) optionButton.active = this.options.waypointsAllowed;
            if (option == EnumOptionsMinimap.CAVEMODE) optionButton.active = this.options.cavesAllowed;
        }

        Button radarOptionsButton = new Button.Builder(Component.translatable("options.minimap.radar"), button -> VoxelConstants.getMinecraft().setScreen(new GuiRadarOptions(this))).bounds(this.getWidth() / 2 - 155, this.getHeight() / 6 + 135 - 6, 150, 20).build();
        radarOptionsButton.active = VoxelConstants.getVoxelMapInstance().getRadarOptions().radarAllowed || VoxelConstants.getVoxelMapInstance().getRadarOptions().radarMobsAllowed || VoxelConstants.getVoxelMapInstance().getRadarOptions().radarPlayersAllowed;
        this.addRenderableWidget(radarOptionsButton);
        this.addRenderableWidget(new Button.Builder(Component.translatable("options.minimap.detailsperformance"), button -> VoxelConstants.getMinecraft().setScreen(new GuiMinimapPerformance(this))).bounds(this.getWidth() / 2 + 5, this.getHeight() / 6 + 135 - 6, 150, 20).build());
        this.addRenderableWidget(new Button.Builder(Component.translatable("options.controls"), button -> VoxelConstants.getMinecraft().setScreen(new GuiMinimapControls(this))).bounds(this.getWidth() / 2 - 155, this.getHeight() / 6 + 159 - 6, 150, 20).build());
        Button worldMapButton = new Button.Builder(Component.translatable("options.minimap.worldmap"), button -> VoxelConstants.getMinecraft().setScreen(new GuiPersistentMapOptions(this))).bounds(this.getWidth() / 2 + 5, this.getHeight() / 6 + 159 - 6, 150, 20).build();
        worldMapButton.active = VoxelMap.mapOptions.worldmapAllowed;
        this.addRenderableWidget(worldMapButton);
        this.addRenderableWidget(new Button.Builder(Component.translatable("gui.done"), button -> VoxelConstants.getMinecraft().setScreen(this.parent)).bounds(this.getWidth() / 2 - 100, this.getHeight() / 6 + 183, 200, 20).build());
    }

    protected void optionClicked(Button par1GuiButton) {
        EnumOptionsMinimap option = ((GuiOptionButtonMinimap) par1GuiButton).returnEnumOptions();
        this.options.setOptionValue(option);
        par1GuiButton.setMessage(Component.literal(this.options.getKeyText(option)));
        if (option == EnumOptionsMinimap.OLDNORTH) {
            VoxelConstants.getVoxelMapInstance().getWaypointManager().setOldNorth(this.options.oldNorth);
        }

    }

    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        this.renderBlurredBackground(drawContext);
        this.renderMenuBackground(drawContext);
        drawContext.drawCenteredString(this.getFontRenderer(), this.screenTitle, this.getWidth() / 2, 20, 0xFFFFFFFF);
        super.render(drawContext, mouseX, mouseY, delta);
    }
}
