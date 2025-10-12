package com.mamiyaotaru.voxelmap.persistent;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.VoxelMap;
import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiOptionButtonMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiOptionSliderMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class GuiPersistentMapOptions extends GuiScreenMinimap {
    private final Screen parent;
    private final PersistentMapSettingsManager options;
    private final Component screenTitle = Component.translatable("options.worldmap.title");
    private final Component cacheSettings = Component.translatable("options.worldmap.cachesettings");
    private final Component warning = Component.translatable("options.worldmap.warning");

    public GuiPersistentMapOptions(Screen parent) {
        this.parent = parent;
        this.setParentScreen(this.parent);

        this.options = VoxelConstants.getVoxelMapInstance().getPersistentMapOptions();
    }

    @Override
    public void init() {
        EnumOptionsMinimap[] relevantOptions = { EnumOptionsMinimap.SHOW_WAYPOINTS, EnumOptionsMinimap.SHOW_WAYPOINT_NAMES};

        int counter = 0;

        for (EnumOptionsMinimap option : relevantOptions) {
            GuiOptionButtonMinimap optionButton = new GuiOptionButtonMinimap(this.getWidth() / 2 - 155 + counter % 2 * 160, this.getHeight() / 6 + 24 * (counter >> 1), option, Component.literal(this.options.getKeyText(option)), this::optionClicked);
            this.addRenderableWidget(optionButton);

            if (option == EnumOptionsMinimap.SHOW_WAYPOINTS) {
                optionButton.active = VoxelMap.mapOptions.waypointsAllowed;
            }
            if (option == EnumOptionsMinimap.SHOW_WAYPOINT_NAMES) {
                optionButton.active = VoxelMap.mapOptions.waypointsAllowed;
            }
            counter++;
        }

        EnumOptionsMinimap[] relevantOptions2 = { EnumOptionsMinimap.MIN_ZOOM, EnumOptionsMinimap.MAX_ZOOM, EnumOptionsMinimap.CACHE_SIZE};
        counter += 2;

        for (EnumOptionsMinimap option : relevantOptions2) {
            if (option.isFloat()) {
                float sValue = this.options.getOptionFloatValue(option);

                this.addRenderableWidget(new GuiOptionSliderMinimap(this.getWidth() / 2 - 155 + counter % 2 * 160, this.getHeight() / 6 + 24 * (counter >> 1), option, switch (option) {
                    case MIN_ZOOM, MAX_ZOOM -> (sValue + 3.0F) / (5 + 3);
                    case CACHE_SIZE -> sValue / 5000.0F;
                    default ->
                            throw new IllegalArgumentException("Add code to handle EnumOptionMinimap: " + option.getName() + ". (possibly not a float value applicable to persistent map)");
                }, this.options));
            } else {
                this.addRenderableWidget(new GuiOptionButtonMinimap(this.getWidth() / 2 - 155 + counter % 2 * 160, this.getHeight() / 6 + 24 * (counter >> 1), option, Component.literal(this.options.getKeyText(option)), this::optionClicked));
            }

            counter++;
        }

        this.addRenderableWidget(new Button.Builder(Component.translatable("gui.done"), buttonx -> VoxelConstants.getMinecraft().setScreen(this.parent)).bounds(this.getWidth() / 2 - 100, this.getHeight() - 28, 200, 20).build());

        for (Object buttonObj : this.children()) {
            if (buttonObj instanceof GuiOptionButtonMinimap button) {
                if (button.returnEnumOptions() == EnumOptionsMinimap.SHOW_WAYPOINT_NAMES) {
                    button.active = this.options.showWaypoints && VoxelMap.mapOptions.waypointsAllowed;
                }
            }
        }

    }

    protected void optionClicked(Button par1GuiButton) {
        EnumOptionsMinimap option = ((GuiOptionButtonMinimap) par1GuiButton).returnEnumOptions();
        this.options.setOptionValue(option);
        par1GuiButton.setMessage(Component.literal(this.options.getKeyText(option)));

        for (Object buttonObj : this.children()) {
            if (buttonObj instanceof GuiOptionButtonMinimap button) {
                if (button.returnEnumOptions() == EnumOptionsMinimap.SHOW_WAYPOINT_NAMES) {
                    button.active = this.options.showWaypoints && VoxelMap.mapOptions.waypointsAllowed;
                }
            }
        }

    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        for (Object buttonObj : this.children()) {
            if (buttonObj instanceof GuiOptionSliderMinimap slider) {
                EnumOptionsMinimap option = slider.returnEnumOptions();
                float sValue = this.options.getOptionFloatValue(option);
                float fValue;

                fValue = switch (option) {
                    case MIN_ZOOM, MAX_ZOOM -> (sValue + 3.0F) / (5 + 3);
                    case CACHE_SIZE -> sValue / 5000.0F;
                    default -> throw new IllegalArgumentException("Add code to handle EnumOptionMinimap: " + option.getName() + ". (possibly not a float value applicable to persistent map)");
                };
                if (this.getFocused() != slider) {
                    slider.setValue(fValue);
                }
            }
        }

        drawContext.drawCenteredString(this.getFont(), this.screenTitle, this.getWidth() / 2, 20, 0xFFFFFFFF);
        drawContext.drawCenteredString(this.getFont(), this.cacheSettings, this.getWidth() / 2, this.getHeight() / 6 + 24, 0xFFFFFFFF);
        drawContext.drawCenteredString(this.getFont(), this.warning, this.getWidth() / 2, this.getHeight() / 6 + 34, 0xFFFFFFFF);
        super.render(drawContext, mouseX, mouseY, delta);
    }
}