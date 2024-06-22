package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiOptionButtonMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import com.mamiyaotaru.voxelmap.persistent.GuiPersistentMapOptions;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.Text;

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
        this.screenTitle = I18n.translate("options.minimap.title");

        for (int i = 0; i < relevantOptions.length; i++) {
            EnumOptionsMinimap option = relevantOptions[i];
            GuiOptionButtonMinimap optionButton = new GuiOptionButtonMinimap(getWidth() / 2 - 155 + i % 2 * 160, getHeight() / 6 + 24 * (i >> 1), option, Text.literal(options.getKeyText(option)), this::optionClicked);
            this.addDrawableChild(optionButton);

            if (option == EnumOptionsMinimap.CAVEMODE) optionButton.active = this.options.cavesAllowed;
        }

        ButtonWidget radarOptionsButton = new ButtonWidget.Builder(Text.translatable("options.minimap.radar"), button -> VoxelConstants.getMinecraft().setScreen(new GuiRadarOptions(this))).dimensions(this.getWidth() / 2 - 155, this.getHeight() / 6 + 135 - 6, 150, 20).build();
        radarOptionsButton.active = VoxelConstants.getVoxelMapInstance().getRadarOptions().radarAllowed || VoxelConstants.getVoxelMapInstance().getRadarOptions().radarMobsAllowed || VoxelConstants.getVoxelMapInstance().getRadarOptions().radarPlayersAllowed;
        this.addDrawableChild(radarOptionsButton);
        this.addDrawableChild(new ButtonWidget.Builder(Text.translatable("options.minimap.detailsperformance"), button -> VoxelConstants.getMinecraft().setScreen(new GuiMinimapPerformance(this))).dimensions(this.getWidth() / 2 + 5, this.getHeight() / 6 + 135 - 6, 150, 20).build());
        this.addDrawableChild(new ButtonWidget.Builder(Text.translatable("options.controls"), button -> VoxelConstants.getMinecraft().setScreen(new GuiMinimapControls(this))).dimensions(this.getWidth() / 2 - 155, this.getHeight() / 6 + 159 - 6, 150, 20).build());
        this.addDrawableChild(new ButtonWidget.Builder(Text.translatable("options.minimap.worldmap"), button -> VoxelConstants.getMinecraft().setScreen(new GuiPersistentMapOptions(this))).dimensions(this.getWidth() / 2 + 5, this.getHeight() / 6 + 159 - 6, 150, 20).build());
        this.addDrawableChild(new ButtonWidget.Builder(Text.translatable("gui.done"), button -> VoxelConstants.getMinecraft().setScreen(this.parent)).dimensions(this.getWidth() / 2 - 100, this.getHeight() / 6 + 183, 200, 20).build());
    }

    protected void optionClicked(ButtonWidget par1GuiButton) {
        EnumOptionsMinimap option = ((GuiOptionButtonMinimap) par1GuiButton).returnEnumOptions();
        this.options.setOptionValue(option);
        par1GuiButton.setMessage(Text.literal(this.options.getKeyText(option)));
        if (option == EnumOptionsMinimap.OLDNORTH) {
            VoxelConstants.getVoxelMapInstance().getWaypointManager().setOldNorth(this.options.oldNorth);
        }

    }

    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        this.applyBlur(delta);
        this.renderDarkening(drawContext);
        drawContext.drawCenteredTextWithShadow(this.getFontRenderer(), this.screenTitle, this.getWidth() / 2, 20, 16777215);
        super.render(drawContext, mouseX, mouseY, delta);
    }
}
