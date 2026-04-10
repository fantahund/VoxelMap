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
    private final Identifier roundHandle = Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "images/color_picker/round_handle.png");
    private final Identifier roundHandleTint = Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "images/color_picker/round_handle_tint.png");
    private final Identifier verticalHandle = Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "images/color_picker/vertical_handle.png");
    private final Identifier verticalHandleTint = Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "images/color_picker/vertical_handle_tint.png");
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
        h = hsv[0];
        s = hsv[1];
        v = hsv[2];

        super.setColor(i);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
        if (mouseButtonEvent.button() != 0) {
            return false;
        }

        pickingHueSat = isHueSatWheelHovered(mouseButtonEvent.x(), mouseButtonEvent.y());
        pickingValue = isValueSliderHovered(mouseButtonEvent.x(), mouseButtonEvent.y());
        if (isPicking()) {
            pickColorAt(mouseButtonEvent.x(), mouseButtonEvent.y());
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double deltaX, double deltaY) {
        if (mouseButtonEvent.button() == 0 && isPicking()) {
            pickColorAt(mouseButtonEvent.x(), mouseButtonEvent.y());
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        if (mouseButtonEvent.button() == 0 && isPicking()) {
            pickingHueSat = false;
            pickingValue = false;
            return true;
        }

        return false;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        int fullColor = color;
        int hsColor = Color.getHSBColor(h, s, 1.0F).getRGB();
        int wheelRadius = getHueSatWheelRadius();

        // render v picker
        int sliderX = getValueSliderX();
        int sliderY = getValueSliderY();
        guiGraphics.fillGradient(sliderX - (SLIDER_WIDTH / 2), sliderY - wheelRadius, sliderX + (SLIDER_WIDTH / 2), sliderY + wheelRadius, hsColor, 0xFF000000);

        float sliderHandleX = sliderX;
        float sliderHandleY = sliderY - wheelRadius + ((wheelRadius * 2.0F) * (1.0F - v));
        VoxelMapGuiGraphics.blitFloat(guiGraphics, RenderPipelines.GUI_TEXTURED, verticalHandle, sliderHandleX - 8, sliderHandleY - 4, 16, 8, 0.0F, 1.0F, 0.0F, 1.0F, 0xFFFFFFFF);
        VoxelMapGuiGraphics.blitFloat(guiGraphics, RenderPipelines.GUI_TEXTURED, verticalHandleTint, sliderHandleX - 8, sliderHandleY - 4, 16, 8, 0.0F, 1.0F, 0.0F, 1.0F, fullColor);

        // render h, s picker
        int wheelX = getHueSatWheelX();
        int wheelY = getHueSatWheelY();
        Identifier colorWheelImage = VoxelConstants.getVoxelMapInstance().getColorManager().getHueSatColorWheel();
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, colorWheelImage, wheelX - wheelRadius, wheelY - wheelRadius, 0.0F, 0.0F, wheelRadius * 2, wheelRadius * 2, wheelRadius * 2, wheelRadius * 2, 0xFFFFFFFF);

        float radians = h * 360.0F * Mth.DEG_TO_RAD;
        float dirX = Mth.cos(radians);
        float dirY = Mth.sin(radians);
        float distance = s * wheelRadius;
        float wheelHandleX = wheelX + (dirX * distance);
        float wheelHandleY = wheelY + (dirY * distance);
        VoxelMapGuiGraphics.blitFloat(guiGraphics, RenderPipelines.GUI_TEXTURED, roundHandle, wheelHandleX - 4, wheelHandleY - 4, 8, 8, 0.0F, 1.0F, 0.0F, 1.0F, 0xFFFFFFFF);
        VoxelMapGuiGraphics.blitFloat(guiGraphics, RenderPipelines.GUI_TEXTURED, roundHandleTint, wheelHandleX - 4, wheelHandleY - 4, 8, 8, 0.0F, 1.0F, 0.0F, 1.0F, hsColor);

        // render texts
        Font font = VoxelConstants.getMinecraft().font;
        guiGraphics.drawString(font, "H/S", wheelX - wheelRadius, wheelY + wheelRadius - 9, 0xFFFFFFFF);
        guiGraphics.drawString(font, "V", sliderX - (SLIDER_WIDTH / 2) - font.width("V") - 4, sliderY + wheelRadius - 9, 0xFFFFFFFF);

    }

    private void pickColorAt(double mouseX, double mouseY) {
        int wheelRadius = getHueSatWheelRadius();

        if (pickingValue) {
            // calculate v
            double dx = getValueSliderX() - mouseX;
            double dy = getValueSliderY() - mouseY;

            v = (float) Mth.clamp((dy + wheelRadius) / (wheelRadius * 2.0), 0.0, 1.0);
        }

        if (pickingHueSat) {
            // calculate h, s
            double dx = getHueSatWheelX() - mouseX;
            double dy = getHueSatWheelY() - mouseY;
            double degrees = Math.toDegrees(Math.atan2(dy, dx)) + 180.0F;
            double distance = Math.sqrt(dx * dx + dy * dy);

            h = (float) Mth.clamp((degrees / 360.0F), 0.0, 1.0);
            s = (float) Mth.clamp((distance / wheelRadius), 0.0, 1.0);
        }

        updateColor(Color.getHSBColor(h, s, v).getRGB());
    }

    private int getHueSatWheelRadius() {
        return getHeight() / 2;
    }

    private int getHueSatWheelX() {
        return getX() - (getWidth() / 2) + getHueSatWheelRadius();
    }

    private int getHueSatWheelY() {
        return getY();
    }

    private int getValueSliderX() {
        return getX() + (getWidth() / 2) - (SLIDER_WIDTH / 2);
    }

    private int getValueSliderY() {
        return getY();
    }

    private boolean isPicking() {
        return pickingHueSat || pickingValue;
    }

    private boolean isHueSatWheelHovered(double mouseX, double mouseY) {
        int wheelRadius = getHueSatWheelRadius();
        double dx = getHueSatWheelX() - mouseX;
        double dy = getHueSatWheelY() - mouseY;

        return (dx * dx + dy * dy) <= (wheelRadius * wheelRadius);
    }

    private boolean isValueSliderHovered(double mouseX, double mouseY) {
        int wheelRadius = getHueSatWheelRadius();
        double dx = getValueSliderX() - mouseX;
        double dy = getValueSliderY() - mouseY;

        return Math.abs(dx) <= (SLIDER_WIDTH / 2.0) && Math.abs(dy) <= wheelRadius;
    }
}
