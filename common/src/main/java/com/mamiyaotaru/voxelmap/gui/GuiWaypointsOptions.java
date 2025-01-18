package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiOptionButtonMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiOptionSliderMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class GuiWaypointsOptions extends GuiScreenMinimap {
    private static final EnumOptionsMinimap[] relevantOptions = { EnumOptionsMinimap.WAYPOINTDISTANCE, EnumOptionsMinimap.DISTANCEUNITCONVERSION, EnumOptionsMinimap.WAYPOINTNAMEBELOWICON, EnumOptionsMinimap.WAYPOINTDISTANCEBELOWNAME, EnumOptionsMinimap.DEATHPOINTS};
    private final Screen parent;
    private final MapSettingsManager options;
    protected Component screenTitle;

    public GuiWaypointsOptions(Screen parent, MapSettingsManager options) {
        this.parent = parent;
        this.options = options;
    }

    public void init() {
        int var2 = 0;
        this.screenTitle = Component.translatable("options.minimap.waypoints.title");

        for (EnumOptionsMinimap option : relevantOptions) {
            if (option.isFloat()) {
                float distance = this.options.getOptionFloatValue(option);
                if (distance < 0.0F) {
                    distance = 10001.0F;
                }

                distance = (distance - 50.0F) / 9951.0F;
                this.addRenderableWidget(new GuiOptionSliderMinimap(this.getWidth() / 2 - 155 + var2 % 2 * 160, this.getHeight() / 6 + 24 * (var2 >> 1), option, distance, this.options));
            } else {
                GuiOptionButtonMinimap optionButton = new GuiOptionButtonMinimap(this.getWidth() / 2 - 155 + var2 % 2 * 160, this.getHeight() / 6 + 24 * (var2 >> 1), option, Component.literal(this.options.getKeyText(option)), this::optionClicked);
                this.addRenderableWidget(optionButton);
            }

            ++var2;
        }

        this.addRenderableWidget(new Button.Builder(Component.translatable("gui.done"), button -> VoxelConstants.getMinecraft().setScreen(this.parent)).bounds(this.getWidth() / 2 - 100, this.getHeight() / 6 + 168, 200, 20).build());
    }

    protected void optionClicked(Button par1GuiButton) {
        EnumOptionsMinimap option = ((GuiOptionButtonMinimap) par1GuiButton).returnEnumOptions();
        this.options.setOptionValue(option);
        par1GuiButton.setMessage(Component.literal(this.options.getKeyText(option)));
    }

    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        this.renderBlurredBackground();
        this.renderMenuBackground(drawContext);
        drawContext.flush();
        drawContext.drawCenteredString(this.font, this.screenTitle, this.getWidth() / 2, 20, 16777215);
        super.render(drawContext, mouseX, mouseY, delta);
    }
}
