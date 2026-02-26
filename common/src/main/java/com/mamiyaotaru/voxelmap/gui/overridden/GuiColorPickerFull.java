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
    private final Identifier verticalHandle = Identifier.fromNamespaceAndPath("voxelmap", "images/color_picker/vertical_handle.png");
    private final Identifier verticalHandleTint = Identifier.fromNamespaceAndPath("voxelmap", "images/color_picker/vertical_handle_tint.png");
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

        this.pickingHue = this.isHueWheelHovered(mouseButtonEvent.x(), mouseButtonEvent.y());
        this.pickingSat = this.isSatSliderHovered(mouseButtonEvent.x(), mouseButtonEvent.y());
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
            this.pickingHue = false;
            this.pickingSat = false;
            this.pickingValue = false;
            return true;
        }

        return false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        int fullColor = this.color;
        int wheelRadius = this.getHueWheelRadius();

        // render s picker
        int sSliderX = this.getSatSliderX();
        int sSliderY = this.getSatSliderY();
        guiGraphics.fillGradient(sSliderX - (SLIDER_WIDTH / 2), sSliderY - wheelRadius, sSliderX + (SLIDER_WIDTH / 2), sSliderY + wheelRadius, Color.getHSBColor(this.h, 1.0F, this.v).getRGB(), Color.getHSBColor(this.h, 0.0F, this.v).getRGB());

        float sSliderHandleX = sSliderX;
        float sSliderHandleY = sSliderY - wheelRadius + ((wheelRadius * 2.0F) * (1.0F - this.s));
        VoxelMapGuiGraphics.blitFloat(guiGraphics, RenderPipelines.GUI_TEXTURED, this.verticalHandle, sSliderHandleX - 8, sSliderHandleY - 4, 16, 8, 0.0F, 1.0F, 0.0F, 1.0F, 0xFFFFFFFF);
        VoxelMapGuiGraphics.blitFloat(guiGraphics, RenderPipelines.GUI_TEXTURED, this.verticalHandleTint, sSliderHandleX - 8, sSliderHandleY - 4, 16, 8, 0.0F, 1.0F, 0.0F, 1.0F, fullColor);

        // render v picker
        int vSliderX = this.getValueSliderX();
        int vSliderY = this.getValueSliderY();
        guiGraphics.fillGradient(vSliderX - (SLIDER_WIDTH / 2), vSliderY - wheelRadius, vSliderX + (SLIDER_WIDTH / 2), vSliderY + wheelRadius, Color.getHSBColor(this.h, this.s, 1.0F).getRGB(), 0xFF000000);

        float vSliderHandleX = vSliderX;
        float vSliderHandleY = vSliderY - wheelRadius + ((wheelRadius * 2.0F) * (1.0F - this.v));
        VoxelMapGuiGraphics.blitFloat(guiGraphics, RenderPipelines.GUI_TEXTURED, this.verticalHandle, vSliderHandleX - 8, vSliderHandleY - 4, 16, 8, 0.0F, 1.0F, 0.0F, 1.0F, 0xFFFFFFFF);
        VoxelMapGuiGraphics.blitFloat(guiGraphics, RenderPipelines.GUI_TEXTURED, this.verticalHandleTint, vSliderHandleX - 8, vSliderHandleY - 4, 16, 8, 0.0F, 1.0F, 0.0F, 1.0F, fullColor);

        // render h, s picker
        int wheelX = this.getHueWheelX();
        int wheelY = this.getHueWheelY();
        Identifier colorWheelImage = VoxelConstants.getVoxelMapInstance().getColorManager().getHueColorWheel();
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, colorWheelImage, wheelX - wheelRadius, wheelY - wheelRadius, 0.0F, 0.0F, wheelRadius * 2, wheelRadius * 2, wheelRadius * 2, wheelRadius * 2, 0xFFFFFFFF);

        float radians = this.h * 360.0F * Mth.DEG_TO_RAD;
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(wheelX, wheelY);
        guiGraphics.pose().rotate(radians);
        guiGraphics.pose().translate(wheelRadius, 0.0F);
        VoxelMapGuiGraphics.blitFloat(guiGraphics, RenderPipelines.GUI_TEXTURED, this.verticalHandle, -SLIDER_WIDTH, -4, 16, 8, 0.0F, 1.0F, 0.0F, 1.0F, 0xFFFFFFFF);
        VoxelMapGuiGraphics.blitFloat(guiGraphics, RenderPipelines.GUI_TEXTURED, this.verticalHandleTint, -SLIDER_WIDTH, -4, 16, 8, 0.0F, 1.0F, 0.0F, 1.0F, Color.getHSBColor(this.h, 1.0F, 1.0F).getRGB());
        guiGraphics.pose().popMatrix();

        // render texts
        Font font = VoxelConstants.getMinecraft().font;
        guiGraphics.drawString(font, "H", wheelX - wheelRadius, wheelY + wheelRadius - 9, 0xFFFFFFFF);
        guiGraphics.drawString(font, "S", sSliderX - (SLIDER_WIDTH / 2) - font.width("S") - 4, sSliderY + wheelRadius - 9, 0xFFFFFFFF);
        guiGraphics.drawString(font, "V", vSliderX - (SLIDER_WIDTH / 2) - font.width("V") - 4, vSliderY + wheelRadius - 9, 0xFFFFFFFF);

    }

    private void pickColorAt(double mouseX, double mouseY) {
        int wheelRadius = this.getHueWheelRadius();

        if (this.pickingValue) {
            // calculate v
            double dx = this.getValueSliderX() - mouseX;
            double dy = this.getValueSliderY() - mouseY;

            this.v = (float) Mth.clamp((dy + wheelRadius) / (wheelRadius * 2.0), 0.0, 1.0);
        }

        if (this.pickingSat) {
            // calculate s
            double dx = this.getSatSliderX() - mouseX;
            double dy = this.getSatSliderY() - mouseY;

            this.s = (float) Mth.clamp((dy + wheelRadius) / (wheelRadius * 2.0), 0.0, 1.0);
        }

        if (this.pickingHue) {
            // calculate h
            double dx = this.getHueWheelX() - mouseX;
            double dy = this.getHueWheelY() - mouseY;
            double degrees = Math.toDegrees(Math.atan2(dy, dx)) + 180.0F;

            this.h = (float) Mth.clamp((degrees / 360.0F), 0.0, 1.0);
        }

        this.updateColor(Color.getHSBColor(this.h, this.s, this.v).getRGB());
    }

    private int getHueWheelRadius() {
        return this.height / 2;
    }

    private int getHueWheelX() {
        return this.x - (this.width / 2) + this.getHueWheelRadius();
    }

    private int getHueWheelY() {
        return this.y;
    }

    private int getSatSliderX() {
        return this.x + (this.width / 2) - SLIDER_WIDTH - 20;
    }

    private int getSatSliderY() {
        return this.y;
    }

    private int getValueSliderX() {
        return this.x + (this.width / 2) - (SLIDER_WIDTH / 2);
    }

    private int getValueSliderY() {
        return this.y;
    }

    private boolean isPicking() {
        return this.pickingHue || this.pickingSat || this.pickingValue;
    }

    private boolean isHueWheelHovered(double mouseX, double mouseY) {
        int wheelRadius = this.getHueWheelRadius();
        int wheelInnerRadius = wheelRadius - SLIDER_WIDTH;
        double dx = this.getHueWheelX() - mouseX;
        double dy = this.getHueWheelY() - mouseY;
        double sqDist = dx * dx + dy * dy;

        return sqDist >= (wheelInnerRadius * wheelInnerRadius) && sqDist <= (wheelRadius * wheelRadius);
    }

    private boolean isSatSliderHovered(double mouseX, double mouseY) {
        int wheelRadius = this.getHueWheelRadius();
        double dx = this.getSatSliderX() - mouseX;
        double dy = this.getSatSliderY() - mouseY;

        return Math.abs(dx) <= (SLIDER_WIDTH / 2.0) && Math.abs(dy) <= wheelRadius;
    }

    private boolean isValueSliderHovered(double mouseX, double mouseY) {
        int wheelRadius = this.getHueWheelRadius();
        double dx = this.getValueSliderX() - mouseX;
        double dy = this.getValueSliderY() - mouseY;

        return Math.abs(dx) <= (SLIDER_WIDTH / 2.0) && Math.abs(dy) <= wheelRadius;
    }
}
