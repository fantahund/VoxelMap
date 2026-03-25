package com.mamiyaotaru.voxelmap.persistent;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiOptionButtonMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiOptionSliderMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class GuiPersistentMapOptions extends GuiScreenMinimap {
    private final PersistentMapSettingsManager options;
    private final MapSettingsManager mapOptions;
    private final Component screenTitle = Component.translatable("options.worldmap.title");
    private final Component cacheSettings = Component.translatable("options.worldmap.cacheSettings");
    private final Component warning = Component.translatable("options.worldmap.warning").withStyle(ChatFormatting.RED);

    public GuiPersistentMapOptions(Screen parent) {
        this.lastScreen = parent;

        this.options = VoxelConstants.getVoxelMapInstance().getPersistentMapOptions();
        this.mapOptions = VoxelConstants.getVoxelMapInstance().getMapOptions();
    }

    @Override
    public void init() {
        EnumOptionsMinimap[] relevantOptions = { EnumOptionsMinimap.SHOW_WORLDMAP_COORDS, EnumOptionsMinimap.SHOW_WAYPOINTS, EnumOptionsMinimap.SHOW_WAYPOINT_NAMES, EnumOptionsMinimap.SHOW_DISTANT_WAYPOINTS };

        int counter = 0;

        for (EnumOptionsMinimap option : relevantOptions) {
            GuiOptionButtonMinimap optionButton = new GuiOptionButtonMinimap(this.getWidth() / 2 - 155 + counter % 2 * 160, this.getHeight() / 6 + 24 * (counter >> 1), option, Component.literal(this.options.getKeyText(option)), this::optionClicked);
            this.addRenderableWidget(optionButton);

            if (option == EnumOptionsMinimap.SHOW_WAYPOINTS) {
                optionButton.active = mapOptions.waypointsAllowed;
            }
            if (option == EnumOptionsMinimap.SHOW_WAYPOINT_NAMES) {
                optionButton.active = mapOptions.waypointsAllowed;
            }
            counter++;
        }

        EnumOptionsMinimap[] relevantOptions2 = { EnumOptionsMinimap.MIN_ZOOM, EnumOptionsMinimap.MAX_ZOOM, EnumOptionsMinimap.CACHE_SIZE };
        counter += (counter % 2 == 0 ? 2 : 3);

        for (EnumOptionsMinimap option : relevantOptions2) {
            if (option.getType() == EnumOptionsMinimap.Type.FLOAT) {
                float sValue = this.options.getFloatValue(option);

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

        this.addRenderableWidget(new Button.Builder(Component.translatable("gui.done"), buttonx -> this.onClose()).bounds(this.getWidth() / 2 - 100, this.getHeight() - 26, 200, 20).build());

        setButtonsActive();
    }

    protected void optionClicked(Button par1GuiButton) {
        EnumOptionsMinimap option = ((GuiOptionButtonMinimap) par1GuiButton).returnEnumOptions();
        MapSettingsManager.updateBooleanOrListValue(this.options, option);
        par1GuiButton.setMessage(Component.literal(this.options.getKeyText(option)));

        setButtonsActive();
    }

    private void setButtonsActive() {
        for (GuiEventListener renderable : this.children()) {
            if (!(renderable instanceof GuiOptionButtonMinimap button)) continue;

            switch (button.returnEnumOptions()) {
                case SHOW_WAYPOINT_NAMES, SHOW_DISTANT_WAYPOINTS -> button.active = options.showWaypoints && mapOptions.waypointsAllowed;
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        for (Object buttonObj : this.children()) {
            if (buttonObj instanceof GuiOptionSliderMinimap slider) {
                EnumOptionsMinimap option = slider.returnEnumOptions();
                float sValue = this.options.getFloatValue(option);
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

        graphics.centeredText(this.getFont(), this.screenTitle, this.getWidth() / 2, 20, 0xFFFFFFFF);
        graphics.centeredText(this.getFont(), this.cacheSettings, this.getWidth() / 2, this.getHeight() / 6 + 49, 0xFFFFFFFF);
        graphics.centeredText(this.getFont(), this.warning, this.getWidth() / 2, this.getHeight() / 6 + 59, 0xFFFFFFFF);

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }
}