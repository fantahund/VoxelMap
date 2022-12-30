package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.RadarSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiOptionButtonMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import com.mamiyaotaru.voxelmap.interfaces.AbstractVoxelMap;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class GuiRadarOptions extends GuiScreenMinimap {
    private static final EnumOptionsMinimap[] relevantOptionsFull = new EnumOptionsMinimap[]{EnumOptionsMinimap.SHOWRADAR, EnumOptionsMinimap.RADARMODE, EnumOptionsMinimap.SHOWHOSTILES, EnumOptionsMinimap.SHOWNEUTRALS, EnumOptionsMinimap.SHOWPLAYERS, EnumOptionsMinimap.SHOWPLAYERNAMES, EnumOptionsMinimap.SHOWMOBNAMES, EnumOptionsMinimap.SHOWPLAYERHELMETS, EnumOptionsMinimap.SHOWMOBHELMETS, EnumOptionsMinimap.RADARFILTERING, EnumOptionsMinimap.RADAROUTLINES};
    private static final EnumOptionsMinimap[] relevantOptionsSimple = new EnumOptionsMinimap[]{EnumOptionsMinimap.SHOWRADAR, EnumOptionsMinimap.RADARMODE, EnumOptionsMinimap.SHOWHOSTILES, EnumOptionsMinimap.SHOWNEUTRALS, EnumOptionsMinimap.SHOWPLAYERS, EnumOptionsMinimap.SHOWFACING};
    private final Screen parent;
    private final RadarSettingsManager options;
    protected Text screenTitle;

    public GuiRadarOptions(Screen parent, AbstractVoxelMap master) {
        this.parent = parent;
        this.options = master.getRadarOptions();
    }

    public void init() {
        this.getButtonList().clear();
        this.children().clear();
        int var2 = 0;
        this.screenTitle = Text.translatable("options.minimap.radar.title");
        EnumOptionsMinimap[] relevantOptions;
        if (this.options.radarMode == 2) {
            relevantOptions = relevantOptionsFull;
        } else {
            relevantOptions = relevantOptionsSimple;
        }

        for (EnumOptionsMinimap option : relevantOptions) {
            GuiOptionButtonMinimap optionButton = new GuiOptionButtonMinimap(this.getWidth() / 2 - 155 + var2 % 2 * 160, this.getHeight() / 6 + 24 * (var2 >> 1), option, Text.literal(this.options.getKeyText(option)), this::optionClicked);
            this.addDrawableChild(optionButton);
            ++var2;
        }

        for (Object buttonObj : this.getButtonList()) {
            if (buttonObj instanceof GuiOptionButtonMinimap button) {
                if (!button.returnEnumOptions().equals(EnumOptionsMinimap.SHOWRADAR)) {
                    button.active = this.options.showRadar;
                }

                if (button.returnEnumOptions().equals(EnumOptionsMinimap.SHOWPLAYERS)) {
                    button.active = button.active && (this.options.radarAllowed || this.options.radarPlayersAllowed);
                } else if (!button.returnEnumOptions().equals(EnumOptionsMinimap.SHOWNEUTRALS) && !button.returnEnumOptions().equals(EnumOptionsMinimap.SHOWHOSTILES)) {
                    if (!button.returnEnumOptions().equals(EnumOptionsMinimap.SHOWPLAYERHELMETS) && !button.returnEnumOptions().equals(EnumOptionsMinimap.SHOWPLAYERNAMES)) {
                        if (button.returnEnumOptions().equals(EnumOptionsMinimap.SHOWMOBHELMETS) && !button.returnEnumOptions().equals(EnumOptionsMinimap.SHOWMOBNAMES)) {
                            button.active = button.active && (this.options.showNeutrals || this.options.showHostiles) && (this.options.radarAllowed || this.options.radarMobsAllowed);
                        }
                    } else {
                        button.active = button.active && this.options.showPlayers && (this.options.radarAllowed || this.options.radarPlayersAllowed);
                    }
                } else {
                    button.active = button.active && (this.options.radarAllowed || this.options.radarMobsAllowed);
                }
            }
        }

        if (this.options.radarMode == 2) {
            this.addDrawableChild(new ButtonWidget(this.getWidth() / 2 - 155, this.getHeight() / 6 + 144 - 6, 150, 20, Text.translatable("options.minimap.radar.selectmobs"), buttonx -> VoxelConstants.getMinecraft().setScreen(new GuiMobs(this, this.options))));
        }

        this.addDrawableChild(new ButtonWidget(this.getWidth() / 2 - 100, this.getHeight() / 6 + 168, 200, 20, Text.translatable("gui.done"), buttonx -> VoxelConstants.getMinecraft().setScreen(this.parent)));
    }

    protected void optionClicked(ButtonWidget buttonClicked) {
        EnumOptionsMinimap option = ((GuiOptionButtonMinimap) buttonClicked).returnEnumOptions();
        this.options.setOptionValue(option);
        if (((GuiOptionButtonMinimap) buttonClicked).returnEnumOptions().equals(EnumOptionsMinimap.RADARMODE)) {
            this.init();
        } else {
            buttonClicked.setMessage(Text.literal(this.options.getKeyText(option)));

            for (Object buttonObj : this.getButtonList()) {
                if (buttonObj instanceof GuiOptionButtonMinimap button) {
                    if (!button.returnEnumOptions().equals(EnumOptionsMinimap.SHOWRADAR)) {
                        button.active = this.options.showRadar;
                    }

                    if (button.returnEnumOptions().equals(EnumOptionsMinimap.SHOWPLAYERS)) {
                        button.active = button.active && (this.options.radarAllowed || this.options.radarPlayersAllowed);
                    } else if (!button.returnEnumOptions().equals(EnumOptionsMinimap.SHOWNEUTRALS) && !button.returnEnumOptions().equals(EnumOptionsMinimap.SHOWHOSTILES)) {
                        if (!button.returnEnumOptions().equals(EnumOptionsMinimap.SHOWPLAYERHELMETS) && !button.returnEnumOptions().equals(EnumOptionsMinimap.SHOWPLAYERNAMES)) {
                            if (button.returnEnumOptions().equals(EnumOptionsMinimap.SHOWMOBHELMETS) && !button.returnEnumOptions().equals(EnumOptionsMinimap.SHOWMOBNAMES)) {
                                button.active = button.active && (this.options.showNeutrals || this.options.showHostiles) && (this.options.radarAllowed || this.options.radarMobsAllowed);
                            }
                        } else {
                            button.active = button.active && this.options.showPlayers && (this.options.radarAllowed || this.options.radarPlayersAllowed);
                        }
                    } else {
                        button.active = button.active && (this.options.radarAllowed || this.options.radarMobsAllowed);
                    }
                }
            }

        }
    }

    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.drawMap(matrixStack);
        this.renderBackground(matrixStack);
        drawCenteredText(matrixStack, this.getFontRenderer(), this.screenTitle, this.getWidth() / 2, 20, 16777215);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }
}
