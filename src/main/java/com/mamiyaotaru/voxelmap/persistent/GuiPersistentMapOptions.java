package com.mamiyaotaru.voxelmap.persistent;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiOptionButtonMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiOptionSliderMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class GuiPersistentMapOptions extends GuiScreenMinimap {
    private final Screen parent;
    private final PersistentMapSettingsManager options;
    private final Text screenTitle = Text.translatable("options.worldmap.title");
    private final Text cacheSettings = Text.translatable("options.worldmap.cachesettings");
    private final Text warning = Text.translatable("options.worldmap.warning");

    public GuiPersistentMapOptions(Screen parent) {
        this.parent = parent;
        this.options = VoxelConstants.getVoxelMapInstance().getPersistentMapOptions();
    }

    public void init() {
        EnumOptionsMinimap[] relevantOptions = { EnumOptionsMinimap.SHOWWAYPOINTS, EnumOptionsMinimap.SHOWWAYPOINTNAMES };

        int counter = 0;

        for (EnumOptionsMinimap option : relevantOptions) {
            this.addDrawableChild(new GuiOptionButtonMinimap(this.getWidth() / 2 - 155 + counter % 2 * 160, this.getHeight() / 6 + 24 * (counter >> 1), option, Text.literal(this.options.getKeyText(option)), this::optionClicked));
            counter++;
        }

        EnumOptionsMinimap[] relevantOptions2 = { EnumOptionsMinimap.MINZOOM, EnumOptionsMinimap.MAXZOOM, EnumOptionsMinimap.CACHESIZE };
        counter += 2;

        for (EnumOptionsMinimap option : relevantOptions2) {
            if (option.isFloat()) {
                float sValue = this.options.getOptionFloatValue(option);

                this.addDrawableChild(new GuiOptionSliderMinimap(this.getWidth() / 2 - 155 + counter % 2 * 160, this.getHeight() / 6 + 24 * (counter >> 1), option, switch (option) {
                    case MINZOOM, MAXZOOM -> (sValue + 3.0F) / (5 + 3);
                    case CACHESIZE -> sValue / 5000.0F;
                    default ->
                            throw new IllegalArgumentException("Add code to handle EnumOptionMinimap: " + option.getName() + ". (possibly not a float value applicable to persistent map)");
                }, this.options));
            } else {
                this.addDrawableChild(new GuiOptionButtonMinimap(this.getWidth() / 2 - 155 + counter % 2 * 160, this.getHeight() / 6 + 24 * (counter >> 1), option, Text.literal(this.options.getKeyText(option)), this::optionClicked));
            }

            counter++;
        }

        this.addDrawableChild(new ButtonWidget.Builder(Text.translatable("gui.done"), buttonx -> VoxelConstants.getMinecraft().setScreen(this.parent)).dimensions(this.getWidth() / 2 - 100, this.getHeight() / 6 + 168, 200, 20).build());

        for (Object buttonObj : this.getButtonList()) {
            if (buttonObj instanceof GuiOptionButtonMinimap button) {
                if (button.returnEnumOptions() == EnumOptionsMinimap.SHOWWAYPOINTNAMES) {
                    button.active = this.options.showWaypoints;
                }
            }
        }

    }

    protected void optionClicked(ButtonWidget par1GuiButton) {
        EnumOptionsMinimap option = ((GuiOptionButtonMinimap) par1GuiButton).returnEnumOptions();
        this.options.setOptionValue(option);
        par1GuiButton.setMessage(Text.literal(this.options.getKeyText(option)));

        for (Object buttonObj : this.getButtonList()) {
            if (buttonObj instanceof GuiOptionButtonMinimap button) {
                if (button.returnEnumOptions() == EnumOptionsMinimap.SHOWWAYPOINTNAMES) {
                    button.active = this.options.showWaypoints;
                }
            }
        }

    }

    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        for (Object buttonObj : this.getButtonList()) {
            if (buttonObj instanceof GuiOptionSliderMinimap slider) {
                EnumOptionsMinimap option = slider.returnEnumOptions();
                float sValue = this.options.getOptionFloatValue(option);
                float fValue;

                fValue = switch (option) {
                    case MINZOOM, MAXZOOM -> (sValue + 3.0F) / (5 + 3);
                    case CACHESIZE -> sValue / 5000.0F;
                    default -> throw new IllegalArgumentException("Add code to handle EnumOptionMinimap: " + option.getName() + ". (possibly not a float value applicable to persistent map)");
                };
                if (this.getFocused() != slider) {
                    slider.setValue(fValue);
                }
            }
        }

        drawMap(drawContext);
        this.renderBackground(drawContext);
        drawContext.drawCenteredTextWithShadow(this.getFontRenderer(), this.screenTitle, this.getWidth() / 2, 20, 16777215);
        drawContext.drawCenteredTextWithShadow(this.getFontRenderer(), this.cacheSettings, this.getWidth() / 2, this.getHeight() / 6 + 24, 16777215);
        drawContext.drawCenteredTextWithShadow(this.getFontRenderer(), this.warning, this.getWidth() / 2, this.getHeight() / 6 + 34, 16777215);
        super.render(drawContext, mouseX, mouseY, delta);
    }
}