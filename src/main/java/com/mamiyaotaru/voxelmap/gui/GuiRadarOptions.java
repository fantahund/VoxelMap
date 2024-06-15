package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.RadarSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiOptionButtonMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class GuiRadarOptions extends GuiScreenMinimap {
    private static final EnumOptionsMinimap[] FULL_RELEVANT_OPTIONS = { EnumOptionsMinimap.SHOWRADAR, EnumOptionsMinimap.RADARMODE, EnumOptionsMinimap.SHOWHOSTILES, EnumOptionsMinimap.SHOWNEUTRALS, EnumOptionsMinimap.SHOWPLAYERS, EnumOptionsMinimap.SHOWPLAYERNAMES, EnumOptionsMinimap.SHOWMOBNAMES, EnumOptionsMinimap.SHOWPLAYERHELMETS, EnumOptionsMinimap.SHOWMOBHELMETS, EnumOptionsMinimap.RADARFILTERING, EnumOptionsMinimap.RADAROUTLINES };
    private static final EnumOptionsMinimap[] SIMPLE_RELEVANT_OPTIONS = { EnumOptionsMinimap.SHOWRADAR, EnumOptionsMinimap.RADARMODE, EnumOptionsMinimap.SHOWHOSTILES, EnumOptionsMinimap.SHOWNEUTRALS, EnumOptionsMinimap.SHOWPLAYERS, EnumOptionsMinimap.SHOWFACING };

    private final Screen parent;
    private final RadarSettingsManager options;
    protected Text screenTitle;

    public GuiRadarOptions(Screen parent) {
        this.parent = parent;
        this.options = VoxelConstants.getVoxelMapInstance().getRadarOptions();
    }

    public void init() {
        clearChildren();
        getButtonList().clear();
        children().clear();

        this.screenTitle = Text.translatable("options.minimap.radar.title");

        EnumOptionsMinimap[] relevantOptions = options.radarMode == 2 ? FULL_RELEVANT_OPTIONS : SIMPLE_RELEVANT_OPTIONS;

        for (int i = 0; i < relevantOptions.length; i++) {
            EnumOptionsMinimap option = relevantOptions[i];
            GuiOptionButtonMinimap optionButton = new GuiOptionButtonMinimap(this.getWidth() / 2 - 155 + i % 2 * 160, this.getHeight() / 6 + 24 * (i >> 1), option, Text.literal(options.getKeyText(option)), this::optionClicked);

            addDrawableChild(optionButton);
        }

        iterateButtonOptions();

        if (options.radarMode == 2) addDrawableChild(new ButtonWidget.Builder(Text.translatable("options.minimap.radar.selectmobs"), x -> VoxelConstants.getMinecraft().setScreen(new GuiMobs(this, options))).dimensions(getWidth() / 2 - 155, getHeight() / 6 + 144 - 6, 150, 20).build());

        addDrawableChild(new ButtonWidget.Builder(Text.translatable("gui.done"), x -> VoxelConstants.getMinecraft().setScreen(parent)).dimensions(getWidth() / 2 - 100, getHeight() / 6 + 168, 200, 20).build());
    }

    protected void optionClicked(ButtonWidget buttonClicked) {
        if (!(buttonClicked instanceof GuiOptionButtonMinimap guiOptionButtonMinimap)) throw new IllegalStateException("Expected GuiOptionMinimap, but received " + buttonClicked.getClass().getSimpleName() + " instead!");

        EnumOptionsMinimap option = guiOptionButtonMinimap.returnEnumOptions();
        options.setOptionValue(option);

        if (guiOptionButtonMinimap.returnEnumOptions() == EnumOptionsMinimap.RADARMODE) {
            init();
            return;
        }

        buttonClicked.setMessage(Text.literal(options.getKeyText(option)));

        iterateButtonOptions();
    }

    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        renderInGameBackground(drawContext);
        drawContext.drawCenteredTextWithShadow(getFontRenderer(), screenTitle, getWidth() / 2, 20, 16777215);

        super.render(drawContext, mouseX, mouseY, delta);
    }

    private void iterateButtonOptions() {
        for (Element element : getButtonList()) {
            if (!(element instanceof GuiOptionButtonMinimap button)) continue;
            if (button.returnEnumOptions() != EnumOptionsMinimap.SHOWRADAR) button.active = options.showRadar;

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

            if (button.returnEnumOptions() == EnumOptionsMinimap.SHOWMOBHELMETS && button.returnEnumOptions() != EnumOptionsMinimap.SHOWMOBNAMES) button.active = button.active && (options.showNeutrals || options.showHostiles) && (options.radarAllowed || options.radarMobsAllowed);
        }
    }
}