package com.mamiyaotaru.voxelmap.gui.overridden;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.util.VoxelMapGuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

import java.awt.Color;

public class GuiColorPickerSimple extends AbstractColorPicker {
    private static final int SLIDER_WIDTH = 14;
    private final Identifier roundHandle = Identifier.fromNamespaceAndPath("voxelmap", "images/color_picker/round_handle.png");
    private final Identifier roundHandleTint = Identifier.fromNamespaceAndPath("voxelmap", "images/color_picker/round_handle_tint.png");
    private final Identifier verticalHandle = Identifier.fromNamespaceAndPath("voxelmap", "images/color_picker/vertical_handle.png");
    private final Identifier verticalHandleTint = Identifier.fromNamespaceAndPath("voxelmap", "images/color_picker/vertical_handle_tint.png");
    private float h;
    private float s;
    private float v;
    private boolean pickingHueSat = false;
    private boolean pickingValue = false;

    public GuiColorPickerSimple(int x, int y, int width, int height, OnColorChange onColorChange) {
        super(x, y, width, height, onColorChange);
    }

    @Override
    public void setColor(int i) {
        int r = ARGB.red(i);
        int g = ARGB.green(i);
        int b = ARGB.blue(i);

        float[] hsv = Color.RGBtoHSB(r, g, b, null);
        this.h = hsv[0];
        this.s = hsv[1];
        this.v = hsv[2];

        super.setColor(i);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
        if (mouseButtonEvent.button() != 0) {
            return false;
        }

        this.pickingHueSat = this.isHueSatWheelHovered(mouseButtonEvent.x(), mouseButtonEvent.y());
        this.pickingValue = this.isValueSliderHovered(mouseButtonEvent.x(), mouseButtonEvent.y());
        if (this.isPicking()) {
            this.pickColorAt(mouseButtonEvent.x(), mouseButtonEvent.y());
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double deltaX, double deltaY) {
        if (mouseButtonEvent.button() == 0 && this.isPicking()) {
            this.pickColorAt(mouseButtonEvent.x(), mouseButtonEvent.y());
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        if (mouseButtonEvent.button() == 0 && this.isPicking()) {
            this.pickingHueSat = false;
            this.pickingValue = false;
            return true;
        }

        return false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        int fullColor = this.color;
        int hsColor = Color.getHSBColor(this.h, this.s, 1.0F).getRGB();
        int wheelRadius = this.getHueSatWheelRadius();

        // render v picker
        int sliderX = this.getValueSliderX();
        int sliderY = this.getValueSliderY();
        guiGraphics.fillGradient(sliderX - (SLIDER_WIDTH / 2), sliderY - wheelRadius, sliderX + (SLIDER_WIDTH / 2), sliderY + wheelRadius, hsColor, 0xFF000000);

        float sliderHandleX = sliderX;
        float sliderHandleY = sliderY - wheelRadius + ((wheelRadius * 2.0F) * (1.0F - this.v));
        VoxelMapGuiGraphics.blitFloat(guiGraphics, RenderPipelines.GUI_TEXTURED, this.verticalHandle, sliderHandleX - 8, sliderHandleY - 4, 16, 8, 0.0F, 1.0F, 0.0F, 1.0F, 0xFFFFFFFF);
        VoxelMapGuiGraphics.blitFloat(guiGraphics, RenderPipelines.GUI_TEXTURED, this.verticalHandleTint, sliderHandleX - 8, sliderHandleY - 4, 16, 8, 0.0F, 1.0F, 0.0F, 1.0F, fullColor);

        // render h, s picker
        int wheelX = this.getHueSatWheelX();
        int wheelY = this.getHueSatWheelY();
        Identifier colorWheelImage = VoxelConstants.getVoxelMapInstance().getColorManager().getHueSatColorWheel();
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, colorWheelImage, wheelX - wheelRadius, wheelY - wheelRadius, 0.0F, 0.0F, wheelRadius * 2, wheelRadius * 2, wheelRadius * 2, wheelRadius * 2, 0xFFFFFFFF);

        float radians = this.h * 360.0F * Mth.DEG_TO_RAD;
        float dirX = Mth.cos(radians);
        float dirY = Mth.sin(radians);
        float distance = this.s * wheelRadius;
        float wheelHandleX = wheelX + (dirX * distance);
        float wheelHandleY = wheelY + (dirY * distance);
        VoxelMapGuiGraphics.blitFloat(guiGraphics, RenderPipelines.GUI_TEXTURED, this.roundHandle, wheelHandleX - 4, wheelHandleY - 4, 8, 8, 0.0F, 1.0F, 0.0F, 1.0F, 0xFFFFFFFF);
        VoxelMapGuiGraphics.blitFloat(guiGraphics, RenderPipelines.GUI_TEXTURED, this.roundHandleTint, wheelHandleX - 4, wheelHandleY - 4, 8, 8, 0.0F, 1.0F, 0.0F, 1.0F, hsColor);

        // render texts
        Font font = VoxelConstants.getMinecraft().font;
        guiGraphics.drawString(font, "H/S", wheelX - wheelRadius, wheelY + wheelRadius - 9, 0xFFFFFFFF);
        guiGraphics.drawString(font, "V", sliderX - (SLIDER_WIDTH / 2) - font.width("V") - 4, sliderY + wheelRadius - 9, 0xFFFFFFFF);

    }

    private void pickColorAt(double mouseX, double mouseY) {
        int wheelRadius = this.getHueSatWheelRadius();

        if (this.pickingValue) {
            // calculate v
            double dx = this.getValueSliderX() - mouseX;
            double dy = this.getValueSliderY() - mouseY;

            this.v = (float) Mth.clamp((dy + wheelRadius) / (wheelRadius * 2.0), 0.0, 1.0);
        }

        if (this.pickingHueSat) {
            // calculate h, s
            double dx = this.getHueSatWheelX() - mouseX;
            double dy = this.getHueSatWheelY() - mouseY;
            double degrees = Math.toDegrees(Math.atan2(dy, dx)) + 180.0F;
            double distance = Math.sqrt(dx * dx + dy * dy);

            this.h = (float) Mth.clamp((degrees / 360.0F), 0.0, 1.0);
            this.s = (float) Mth.clamp((distance / wheelRadius), 0.0, 1.0);
        }

        this.updateColor(Color.getHSBColor(this.h, this.s, this.v).getRGB());
    }

    private int getHueSatWheelRadius() {
        return this.height / 2;
    }

    private int getHueSatWheelX() {
        return this.x - (this.width / 2) + this.getHueSatWheelRadius();
    }

    private int getHueSatWheelY() {
        return this.y;
    }

    private int getValueSliderX() {
        return this.x + (this.width / 2) - (SLIDER_WIDTH / 2);
    }

    private int getValueSliderY() {
        return this.y;
    }

    private boolean isPicking() {
        return this.pickingHueSat || this.pickingValue;
    }

    private boolean isHueSatWheelHovered(double mouseX, double mouseY) {
        int wheelRadius = this.getHueSatWheelRadius();
        double dx = this.getHueSatWheelX() - mouseX;
        double dy = this.getHueSatWheelY() - mouseY;

        return (dx * dx + dy * dy) <= (wheelRadius * wheelRadius);
    }

    private boolean isValueSliderHovered(double mouseX, double mouseY) {
        int wheelRadius = this.getHueSatWheelRadius();
        double dx = this.getValueSliderX() - mouseX;
        double dy = this.getValueSliderY() - mouseY;

        return Math.abs(dx) <= (SLIDER_WIDTH / 2.0) && Math.abs(dy) <= wheelRadius;
    }
}
