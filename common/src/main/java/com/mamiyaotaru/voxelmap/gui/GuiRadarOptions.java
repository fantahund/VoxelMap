package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.RadarSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiOptionButtonMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class GuiRadarOptions extends GuiScreenMinimap {
    private static final EnumOptionsMinimap[] FULL_RELEVANT_OPTIONS = { EnumOptionsMinimap.SHOWRADAR, EnumOptionsMinimap.RADARMODE,  EnumOptionsMinimap.SHOWNEUTRALS, EnumOptionsMinimap.SHOWHOSTILES, EnumOptionsMinimap.SHOWPLAYERS, EnumOptionsMinimap.SHOWMOBNAMES, EnumOptionsMinimap.SHOWPLAYERNAMES, EnumOptionsMinimap.SHOWMOBHELMETS, EnumOptionsMinimap.SHOWPLAYERHELMETS, EnumOptionsMinimap.RADARFILTERING, EnumOptionsMinimap.RADAROUTLINES };
    private static final EnumOptionsMinimap[] SIMPLE_RELEVANT_OPTIONS = { EnumOptionsMinimap.SHOWRADAR, EnumOptionsMinimap.RADARMODE, EnumOptionsMinimap.SHOWNEUTRALS, EnumOptionsMinimap.SHOWHOSTILES, EnumOptionsMinimap.SHOWPLAYERS, EnumOptionsMinimap.SHOWFACING };

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

            addRenderableWidget(optionButton);
        }

        iterateButtonOptions();

        if (options.radarMode == 2) {
            addRenderableWidget(new Button.Builder(Component.translatable("options.minimap.radar.selectmobs"), x -> VoxelConstants.getMinecraft().setScreen(new GuiMobs(this, options))).bounds(getWidth() / 2 + 5, getHeight() / 6 + 120, 150, 20).build());
        }

        addRenderableWidget(new Button.Builder(Component.translatable("gui.done"), x -> VoxelConstants.getMinecraft().setScreen(parent)).bounds(getWidth() / 2 - 100, getHeight() - 28, 200, 20).build());
    }

    protected void optionClicked(Button buttonClicked) {
        if (!(buttonClicked instanceof GuiOptionButtonMinimap guiOptionButtonMinimap)) {
            throw new IllegalStateException("Expected GuiOptionMinimap, but received " + buttonClicked.getClass().getSimpleName() + " instead!");
        }

        EnumOptionsMinimap option = guiOptionButtonMinimap.returnEnumOptions();
        options.setOptionValue(option);

        if (guiOptionButtonMinimap.returnEnumOptions() == EnumOptionsMinimap.RADARMODE) {
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
            if (button.returnEnumOptions() != EnumOptionsMinimap.SHOWRADAR) {
                button.active = options.showRadar;
            }

            if (button.returnEnumOptions() == EnumOptionsMinimap.SHOWPLAYERS) {
                button.active = button.active && (options.radarAllowed || options.radarPlayersAllowed);
                continue;
            }

            if (!(button.returnEnumOptions() != EnumOptionsMinimap.SHOWNEUTRALS && button.returnEnumOptions() != EnumOptionsMinimap.SHOWHOSTILES)) {
                button.active = button.active && (options.radarAllowed || options.radarMobsAllowed);
                continue;
            }

            if (!(button.returnEnumOptions() != EnumOptionsMinimap.SHOWPLAYERHELMETS && button.returnEnumOptions() != EnumOptionsMinimap.SHOWPLAYERNAMES)) {
                button.active = button.active && options.showPlayers && (options.radarAllowed || options.radarPlayersAllowed);
                continue;
            }

            if (button.returnEnumOptions() == EnumOptionsMinimap.SHOWMOBHELMETS && button.returnEnumOptions() != EnumOptionsMinimap.SHOWMOBNAMES) {
                button.active = button.active && (options.showNeutrals || options.showHostiles) && (options.radarAllowed || options.radarMobsAllowed);
            }
        }
    }
}