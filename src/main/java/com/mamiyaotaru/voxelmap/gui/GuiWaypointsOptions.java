package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiOptionButtonMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiOptionSliderMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

public class GuiWaypointsOptions extends GuiScreenMinimap {
    private static final EnumOptionsMinimap[] relevantOptions = new EnumOptionsMinimap[]{EnumOptionsMinimap.WAYPOINTDISTANCE, EnumOptionsMinimap.DEATHPOINTS};
    private final Screen parent;
    private final MapSettingsManager options;
    protected Text screenTitle;

    public GuiWaypointsOptions(Screen parent, MapSettingsManager options) {
        this.parent = parent;
        this.options = options;
    }

    public void init() {
        int var2 = 0;
        this.screenTitle = new TranslatableText("options.minimap.waypoints.title");

        for (EnumOptionsMinimap option : relevantOptions) {
            if (option.isFloat()) {
                float distance = this.options.getOptionFloatValue(option);
                if (distance < 0.0F) {
                    distance = 10001.0F;
                }

                distance = (distance - 50.0F) / 9951.0F;
                this.addDrawableChild(new GuiOptionSliderMinimap(this.getWidth() / 2 - 155 + var2 % 2 * 160, this.getHeight() / 6 + 24 * (var2 >> 1), option, distance, this.options));
            } else {
                GuiOptionButtonMinimap optionButton = new GuiOptionButtonMinimap(this.getWidth() / 2 - 155 + var2 % 2 * 160, this.getHeight() / 6 + 24 * (var2 >> 1), option, new LiteralText(this.options.getKeyText(option)), this::optionClicked);
                this.addDrawableChild(optionButton);
            }

            ++var2;
        }

        this.addDrawableChild(new ButtonWidget(this.getWidth() / 2 - 100, this.getHeight() / 6 + 168, 200, 20, new TranslatableText("gui.done"), button -> this.getMinecraft().setScreen(this.parent)));
    }

    protected void optionClicked(ButtonWidget par1GuiButton) {
        EnumOptionsMinimap option = ((GuiOptionButtonMinimap) par1GuiButton).returnEnumOptions();
        this.options.setOptionValue(option);
        par1GuiButton.setMessage(new LiteralText(this.options.getKeyText(option)));
    }

    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.drawMap(matrixStack);
        this.renderBackground(matrixStack);
        drawCenteredText(matrixStack, this.textRenderer, this.screenTitle, this.getWidth() / 2, 20, 16777215);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }
}
