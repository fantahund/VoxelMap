package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.RadarSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiOptionButtonMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class GuiRadarOptions extends GuiScreenMinimap {
    private static final EnumOptionsMinimap[] FULL_RELEVANT_OPTIONS = { EnumOptionsMinimap.SHOW_RADAR, EnumOptionsMinimap.RADAR_MODE,  EnumOptionsMinimap.SHOW_MOBS, EnumOptionsMinimap.SHOW_PLAYERS, EnumOptionsMinimap.SHOW_MOB_NAMES, EnumOptionsMinimap.SHOW_PLAYER_NAMES, EnumOptionsMinimap.SHOW_MOB_HELMETS, EnumOptionsMinimap.SHOW_PLAYER_HELMETS, EnumOptionsMinimap.RADAR_FILTERING, EnumOptionsMinimap.RADAR_OUTLINES};
    private static final EnumOptionsMinimap[] SIMPLE_RELEVANT_OPTIONS = { EnumOptionsMinimap.SHOW_RADAR, EnumOptionsMinimap.RADAR_MODE, EnumOptionsMinimap.SHOW_MOBS, EnumOptionsMinimap.SHOW_PLAYERS, EnumOptionsMinimap.SHOW_FACING};

    private final Screen parent;
    private final RadarSettingsManager options;
    protected Component screenTitle;

    public GuiRadarOptions(Screen parent) {
        this.parent = parent;
        this.setParentScreen(this.parent);

        this.options = VoxelConstants.getVoxelMapInstance().getRadarOptions();
    }

    @Override
    public void init() {
        clearWidgets();
        children().clear();

        this.screenTitle = Component.translatable("options.minimap.radar.title");

        EnumOptionsMinimap[] relevantOptions = options.radarMode == 2 ? FULL_RELEVANT_OPTIONS : SIMPLE_RELEVANT_OPTIONS;

        for (int i = 0; i < relevantOptions.length; i++) {
            EnumOptionsMinimap option = relevantOptions[i];
            GuiOptionButtonMinimap optionButton = new GuiOptionButtonMinimap(this.getWidth() / 2 - 155 + i % 2 * 160, this.getHeight() / 6 + 24 * (i >> 1), option, Component.literal(options.getKeyText(option)), this::optionClicked);

            // TODO: remove this code after radar helmet icon implementation
            switch (option) {
                case SHOW_MOB_HELMETS, SHOW_PLAYER_HELMETS -> optionButton.setTooltip(Tooltip.create(Component.translatable("minimap.ui.workInProgress")));
            }
            //

            addRenderableWidget(optionButton);
        }

        iterateButtonOptions();

        if (options.radarMode == 2) {
            addRenderableWidget(new Button.Builder(Component.translatable("options.minimap.radar.selectMobs"), x -> VoxelConstants.getMinecraft().setScreen(new GuiMobs(this, options))).bounds(getWidth() / 2 - 155, getHeight() / 6 + 135 - 6, 150, 20).build());
        }

        addRenderableWidget(new Button.Builder(Component.translatable("gui.done"), x -> VoxelConstants.getMinecraft().setScreen(parent)).bounds(getWidth() / 2 - 100, getHeight() - 28, 200, 20).build());
    }

    protected void optionClicked(Button buttonClicked) {
        if (!(buttonClicked instanceof GuiOptionButtonMinimap guiOptionButtonMinimap)) {
            throw new IllegalStateException("Expected GuiOptionMinimap, but received " + buttonClicked.getClass().getSimpleName() + " instead!");
        }

        EnumOptionsMinimap option = guiOptionButtonMinimap.returnEnumOptions();
        options.setOptionValue(option);

        if (guiOptionButtonMinimap.returnEnumOptions() == EnumOptionsMinimap.RADAR_MODE) {
            init();
            return;
        }

        buttonClicked.setMessage(Component.literal(options.getKeyText(option)));

        iterateButtonOptions();
    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        drawContext.drawCenteredString(getFont(), screenTitle, getWidth() / 2, 20, 0xFFFFFFFF);

        super.render(drawContext, mouseX, mouseY, delta);
    }

    private void iterateButtonOptions() {
        for (GuiEventListener element : children()) {
            if (!(element instanceof GuiOptionButtonMinimap button)) {
                continue;
            }

            if (button.returnEnumOptions() != EnumOptionsMinimap.SHOW_RADAR) {
                button.active = options.showRadar;
            }

            if (button.returnEnumOptions() == EnumOptionsMinimap.SHOW_PLAYERS) {
                button.active = options.radarAllowed || options.radarPlayersAllowed;
                continue;
            }

            if (button.returnEnumOptions() == EnumOptionsMinimap.SHOW_MOBS) {
                button.active = options.radarAllowed || options.radarMobsAllowed;
                continue;
            }

            if (button.returnEnumOptions() == EnumOptionsMinimap.SHOW_PLAYER_HELMETS || button.returnEnumOptions() == EnumOptionsMinimap.SHOW_PLAYER_NAMES) {
                button.active = options.showPlayers && (options.radarAllowed || options.radarPlayersAllowed);
                // TODO: remove this code after radar helmet icon implementation
                if (button.returnEnumOptions() == EnumOptionsMinimap.SHOW_PLAYER_HELMETS) {
                    button.active = false;
                }
                //
                continue;
            }

            if (button.returnEnumOptions() == EnumOptionsMinimap.SHOW_MOB_HELMETS || button.returnEnumOptions() == EnumOptionsMinimap.SHOW_MOB_NAMES) {
                button.active = (options.showNeutrals || options.showHostiles) && (options.radarAllowed || options.radarMobsAllowed);
                // TODO: remove this code after radar helmet icon implementation
                if (button.returnEnumOptions() == EnumOptionsMinimap.SHOW_MOB_HELMETS) {
                    button.active = false;
                }
                //
            }
        }
    }
}