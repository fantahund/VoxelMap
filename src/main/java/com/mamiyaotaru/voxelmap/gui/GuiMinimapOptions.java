package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiOptionButtonMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import com.mamiyaotaru.voxelmap.interfaces.IVoxelMap;
import com.mamiyaotaru.voxelmap.persistent.GuiPersistentMapOptions;
import com.mamiyaotaru.voxelmap.util.I18nUtils;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;

public class GuiMinimapOptions extends GuiScreenMinimap {
    private Screen parent;
    private IVoxelMap master;
    private static EnumOptionsMinimap[] relevantOptions;
    private final MapSettingsManager options;
    protected String screenTitle = "Minimap Options";

    public GuiMinimapOptions(Screen parent, IVoxelMap master) {
        this.parent = parent;
        this.master = master;
        this.options = master.getMapOptions();
    }

    public void init() {
        relevantOptions = new EnumOptionsMinimap[]{EnumOptionsMinimap.COORDS, EnumOptionsMinimap.HIDE, EnumOptionsMinimap.LOCATION, EnumOptionsMinimap.SIZE, EnumOptionsMinimap.SQUARE, EnumOptionsMinimap.ROTATES, EnumOptionsMinimap.BEACONS, EnumOptionsMinimap.CAVEMODE};
        int var2 = 0;
        this.screenTitle = I18nUtils.getString("options.minimap.title");

        for (int t = 0; t < relevantOptions.length; ++t) {
            EnumOptionsMinimap option = relevantOptions[t];
            GuiOptionButtonMinimap optionButton = new GuiOptionButtonMinimap(this.getWidth() / 2 - 155 + var2 % 2 * 160, this.getHeight() / 6 + 24 * (var2 >> 1), option, new LiteralText(this.options.getKeyText(option)), button -> this.optionClicked(button));
            this.addDrawableChild(optionButton);
            if (option.equals(EnumOptionsMinimap.CAVEMODE)) {
                optionButton.active = this.options.cavesAllowed;
            }

            ++var2;
        }

        ButtonWidget radarOptionsButton = new ButtonWidget(this.getWidth() / 2 - 155, this.getHeight() / 6 + 120 - 6, 150, 20, new TranslatableText("options.minimap.radar"), button -> this.getMinecraft().setScreen(new GuiRadarOptions(this, this.master)));
        radarOptionsButton.active = this.master.getRadarOptions().radarAllowed || this.master.getRadarOptions().radarMobsAllowed || this.master.getRadarOptions().radarPlayersAllowed;
        this.addDrawableChild(radarOptionsButton);
        this.addDrawableChild(new ButtonWidget(this.getWidth() / 2 + 5, this.getHeight() / 6 + 120 - 6, 150, 20, new TranslatableText("options.minimap.detailsperformance"), button -> this.getMinecraft().setScreen(new GuiMinimapPerformance(this, this.master))));
        this.addDrawableChild(new ButtonWidget(this.getWidth() / 2 - 155, this.getHeight() / 6 + 144 - 6, 150, 20, new TranslatableText("options.controls"), button -> this.getMinecraft().setScreen(new GuiMinimapControls(this, this.master))));
        this.addDrawableChild(new ButtonWidget(this.getWidth() / 2 + 5, this.getHeight() / 6 + 144 - 6, 150, 20, new TranslatableText("options.minimap.worldmap"), button -> this.getMinecraft().setScreen(new GuiPersistentMapOptions(this, this.master))));
        this.addDrawableChild(new ButtonWidget(this.getWidth() / 2 - 100, this.getHeight() / 6 + 168, 200, 20, new TranslatableText("gui.done"), button -> this.getMinecraft().setScreen(this.parent)));
    }

    protected void optionClicked(ButtonWidget par1GuiButton) {
        EnumOptionsMinimap option = ((GuiOptionButtonMinimap) par1GuiButton).returnEnumOptions();
        this.options.setOptionValue(option);
        par1GuiButton.setMessage(new LiteralText(this.options.getKeyText(option)));
        if (option == EnumOptionsMinimap.OLDNORTH) {
            this.master.getWaypointManager().setOldNorth(this.options.oldNorth);
        }

    }

    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.drawMap(matrixStack);
        this.renderBackground(matrixStack);
        drawCenteredText(matrixStack, this.getFontRenderer(), this.screenTitle, this.getWidth() / 2, 20, 16777215);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }
}
