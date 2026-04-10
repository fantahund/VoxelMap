package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiOptionButtonMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiOptionSliderMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class GuiWaypointsOptions extends GuiScreenMinimap {
    private static final EnumOptionsMinimap[] relevantOptions = { EnumOptionsMinimap.WAYPOINT_DISTANCE, EnumOptionsMinimap.WAYPOINT_SIGN_SCALE, EnumOptionsMinimap.DEATHPOINTS, EnumOptionsMinimap.WAYPOINT_DISTANCE_UNIT_CONVERSION, EnumOptionsMinimap.WAYPOINT_LABEL_STYLE, EnumOptionsMinimap.HIGHLIGHT_FOCUSED_WAYPOINT };
    private final MapSettingsManager options;

    public GuiWaypointsOptions(Screen parentGui) {
        super(parentGui, Component.translatable("options.minimap.waypoints.title"));
        options = VoxelConstants.getVoxelMapInstance().getMapOptions();
    }

    @Override
    public void init() {
        int var2 = 0;

        for (EnumOptionsMinimap option : relevantOptions) {
            if (option.getType() == EnumOptionsMinimap.Type.FLOAT) {
                float value = options.getFloatValue(option);
                switch (option) {
                    case WAYPOINT_DISTANCE -> {
                        if (value < 0.0F) {
                            value = 10001.0F;
                        }

                        value = (value - 50.0F) / 9951.0F;
                    }
                    case WAYPOINT_SIGN_SCALE -> value = value - 0.5F;
                }
                addRenderableWidget(new GuiOptionSliderMinimap(getWidth() / 2 - 155 + var2 % 2 * 160, getHeight() / 6 + 24 * (var2 >> 1), option, value, options));
            } else {
                GuiOptionButtonMinimap optionButton = new GuiOptionButtonMinimap(getWidth() / 2 - 155 + var2 % 2 * 160, getHeight() / 6 + 24 * (var2 >> 1), option, Component.literal(options.getKeyText(option)), this::optionClicked);

                switch (option) {
                    case DEATHPOINTS -> optionButton.setTooltip(Tooltip.create(Component.translatable("options.minimap.waypoints.deathpoints.tooltip")));
                    case WAYPOINT_DISTANCE_UNIT_CONVERSION -> optionButton.setTooltip(Tooltip.create(Component.translatable("options.minimap.waypoints.distanceUnitConversion.tooltip")));
                }

                addRenderableWidget(optionButton);
            }

            ++var2;
        }

        setButtonsActive();

        this.addRenderableWidget(new Button.Builder(Component.translatable("gui.done"), button -> onClose()).bounds(getWidth() / 2 - 100, getHeight() - 26, 200, 20).build());
    }

    protected void optionClicked(Button par1GuiButton) {
        EnumOptionsMinimap option = ((GuiOptionButtonMinimap) par1GuiButton).returnEnumOptions();
        MapSettingsManager.updateBooleanOrListValue(options, option);
        par1GuiButton.setMessage(Component.literal(options.getKeyText(option)));

        setButtonsActive();
    }

    private void setButtonsActive() {
        for (GuiEventListener item : children()) {
            if (item instanceof GuiOptionButtonMinimap button) {
                EnumOptionsMinimap option = button.returnEnumOptions();

                switch (option) {
                    case WAYPOINT_LABEL_STYLE, HIGHLIGHT_FOCUSED_WAYPOINT, WAYPOINT_DISTANCE_UNIT_CONVERSION -> {
                        button.active = button.active && options.showWaypointSigns;
                    }
                }
            } else if (item instanceof GuiOptionSliderMinimap slider) {
                EnumOptionsMinimap option = slider.returnEnumOptions();

                if (option == EnumOptionsMinimap.WAYPOINT_SIGN_SCALE) {
                    slider.active = options.showWaypointSigns;
                }
            }
        }
    }
}
