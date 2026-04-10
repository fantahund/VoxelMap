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

public class GuiColorPickerFull extends AbstractColorPicker {
    private static final int SLIDER_WIDTH = 14;
    private final Identifier sliderHandle = Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "images/color_picker/slider_handle.png");
    private final Identifier sliderHandleTint = Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "images/color_picker/slider_handle_tint.png");
    private float h;
    private float s;
    private float v;
    private boolean pickingHue = false;
    private boolean pickingSat = false;
    private boolean pickingValue = false;

    public GuiColorPickerFull(int x, int y, int width, int height, OnColorChange onColorChange) {
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

        pickingHue = isHueWheelHovered(mouseButtonEvent.x(), mouseButtonEvent.y());
        pickingSat = isSatSliderHovered(mouseButtonEvent.x(), mouseButtonEvent.y());
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
            pickingHue = false;
            pickingSat = false;
            pickingValue = false;
            return true;
        }

        return false;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        int fullColor = color;
        int wheelRadius = getHueWheelRadius();

        // render s picker
        int sSliderX = getSatSliderX();
        int sSliderY = getSatSliderY();
        guiGraphics.fillGradient(sSliderX - (SLIDER_WIDTH / 2), sSliderY - wheelRadius, sSliderX + (SLIDER_WIDTH / 2), sSliderY + wheelRadius, Color.getHSBColor(h, 1.0F, v).getRGB(), Color.getHSBColor(h, 0.0F, v).getRGB());

        float sSliderHandleX = sSliderX;
        float sSliderHandleY = sSliderY - wheelRadius + ((wheelRadius * 2.0F) * (1.0F - s));
        VoxelMapGuiGraphics.blitFloat(guiGraphics, RenderPipelines.GUI_TEXTURED, sliderHandle, sSliderHandleX - 8, sSliderHandleY - 4, 16, 8, 0.0F, 1.0F, 0.0F, 1.0F, 0xFFFFFFFF);
        VoxelMapGuiGraphics.blitFloat(guiGraphics, RenderPipelines.GUI_TEXTURED, sliderHandleTint, sSliderHandleX - 8, sSliderHandleY - 4, 16, 8, 0.0F, 1.0F, 0.0F, 1.0F, fullColor);

        // render v picker
        int vSliderX = getValueSliderX();
        int vSliderY = getValueSliderY();
        guiGraphics.fillGradient(vSliderX - (SLIDER_WIDTH / 2), vSliderY - wheelRadius, vSliderX + (SLIDER_WIDTH / 2), vSliderY + wheelRadius, Color.getHSBColor(h, s, 1.0F).getRGB(), 0xFF000000);

        float vSliderHandleX = vSliderX;
        float vSliderHandleY = vSliderY - wheelRadius + ((wheelRadius * 2.0F) * (1.0F - v));
        VoxelMapGuiGraphics.blitFloat(guiGraphics, RenderPipelines.GUI_TEXTURED, sliderHandle, vSliderHandleX - 8, vSliderHandleY - 4, 16, 8, 0.0F, 1.0F, 0.0F, 1.0F, 0xFFFFFFFF);
        VoxelMapGuiGraphics.blitFloat(guiGraphics, RenderPipelines.GUI_TEXTURED, sliderHandleTint, vSliderHandleX - 8, vSliderHandleY - 4, 16, 8, 0.0F, 1.0F, 0.0F, 1.0F, fullColor);

        // render h, s picker
        int wheelX = getHueWheelX();
        int wheelY = getHueWheelY();
        Identifier colorWheelImage = VoxelConstants.getVoxelMapInstance().getColorManager().getHueColorWheel();
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, colorWheelImage, wheelX - wheelRadius, wheelY - wheelRadius, 0.0F, 0.0F, wheelRadius * 2, wheelRadius * 2, wheelRadius * 2, wheelRadius * 2, 0xFFFFFFFF);

        float radians = h * 360.0F * Mth.DEG_TO_RAD;
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(wheelX, wheelY);
        guiGraphics.pose().rotate(radians);
        guiGraphics.pose().translate(wheelRadius, 0.0F);
        VoxelMapGuiGraphics.blitFloat(guiGraphics, RenderPipelines.GUI_TEXTURED, sliderHandle, -SLIDER_WIDTH, -4, 16, 8, 0.0F, 1.0F, 0.0F, 1.0F, 0xFFFFFFFF);
        VoxelMapGuiGraphics.blitFloat(guiGraphics, RenderPipelines.GUI_TEXTURED, sliderHandleTint, -SLIDER_WIDTH, -4, 16, 8, 0.0F, 1.0F, 0.0F, 1.0F, Color.getHSBColor(h, 1.0F, 1.0F).getRGB());
        guiGraphics.pose().popMatrix();

        // render texts
        Font font = VoxelConstants.getMinecraft().font;
        guiGraphics.drawString(font, "H", wheelX - wheelRadius, wheelY + wheelRadius - 9, 0xFFFFFFFF);
        guiGraphics.drawString(font, "S", sSliderX - (SLIDER_WIDTH / 2) - font.width("S") - 4, sSliderY + wheelRadius - 9, 0xFFFFFFFF);
        guiGraphics.drawString(font, "V", vSliderX - (SLIDER_WIDTH / 2) - font.width("V") - 4, vSliderY + wheelRadius - 9, 0xFFFFFFFF);

    }

    private void pickColorAt(double mouseX, double mouseY) {
        int wheelRadius = getHueWheelRadius();

        if (pickingValue) {
            // calculate v
            double dx = getValueSliderX() - mouseX;
            double dy = getValueSliderY() - mouseY;

            v = (float) Mth.clamp((dy + wheelRadius) / (wheelRadius * 2.0), 0.0, 1.0);
        }

        if (pickingSat) {
            // calculate s
            double dx = getSatSliderX() - mouseX;
            double dy = getSatSliderY() - mouseY;

            s = (float) Mth.clamp((dy + wheelRadius) / (wheelRadius * 2.0), 0.0, 1.0);
        }

        if (pickingHue) {
            // calculate h
            double dx = getHueWheelX() - mouseX;
            double dy = getHueWheelY() - mouseY;
            double degrees = Math.toDegrees(Math.atan2(dy, dx)) + 180.0F;

            h = (float) Mth.clamp((degrees / 360.0F), 0.0, 1.0);
        }

        updateColor(Color.getHSBColor(h, s, v).getRGB());
    }

    private int getHueWheelRadius() {
        return getHeight() / 2;
    }

    private int getHueWheelX() {
        return getX() - (getWidth() / 2) + getHueWheelRadius();
    }

    private int getHueWheelY() {
        return getY();
    }

    private int getSatSliderX() {
        return getX() + (getWidth() / 2) - SLIDER_WIDTH - 20;
    }

    private int getSatSliderY() {
        return getY();
    }

    private int getValueSliderX() {
        return getX() + (getWidth() / 2) - (SLIDER_WIDTH / 2);
    }

    private int getValueSliderY() {
        return getY();
    }

    private boolean isPicking() {
        return pickingHue || pickingSat || pickingValue;
    }

    private boolean isHueWheelHovered(double mouseX, double mouseY) {
        int wheelRadius = getHueWheelRadius();
        int wheelInnerRadius = wheelRadius - SLIDER_WIDTH;
        double dx = getHueWheelX() - mouseX;
        double dy = getHueWheelY() - mouseY;
        double sqDist = dx * dx + dy * dy;

        return sqDist >= (wheelInnerRadius * wheelInnerRadius) && sqDist <= (wheelRadius * wheelRadius);
    }

    private boolean isSatSliderHovered(double mouseX, double mouseY) {
        int wheelRadius = getHueWheelRadius();
        double dx = getSatSliderX() - mouseX;
        double dy = getSatSliderY() - mouseY;

        return Math.abs(dx) <= (SLIDER_WIDTH / 2.0) && Math.abs(dy) <= wheelRadius;
    }

    private boolean isValueSliderHovered(double mouseX, double mouseY) {
        int wheelRadius = getHueWheelRadius();
        double dx = getValueSliderX() - mouseX;
        double dy = getValueSliderY() - mouseY;

        return Math.abs(dx) <= (SLIDER_WIDTH / 2.0) && Math.abs(dy) <= wheelRadius;
    }
}
