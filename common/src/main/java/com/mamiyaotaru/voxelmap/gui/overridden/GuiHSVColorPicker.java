package com.mamiyaotaru.voxelmap.gui.overridden;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.util.VoxelMapGuiGraphics;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

import java.awt.Color;

public class GuiHSVColorPicker {
    private final Identifier roundHandle = Identifier.fromNamespaceAndPath("voxelmap", "images/color_picker/round_handle.png");
    private final Identifier roundHandleTint = Identifier.fromNamespaceAndPath("voxelmap", "images/color_picker/round_handle_tint.png");
    private final Identifier verticalHandle = Identifier.fromNamespaceAndPath("voxelmap", "images/color_picker/vertical_handle.png");
    private final Identifier verticalHandleTint = Identifier.fromNamespaceAndPath("voxelmap", "images/color_picker/vertical_handle_tint.png");
    private final int sliderWidth;
    private final OnColorChange onColorChange;
    private int x;
    private int y;
    private int width;
    private int height;
    private float h;
    private float s;
    private float v;
    private int color;
    private boolean pickingColor = false;
    private boolean pickingValue = false;

    public GuiHSVColorPicker(int x, int y, int width, int height, int sliderWidth, OnColorChange onColorChange) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.sliderWidth = sliderWidth;
        this.onColorChange = onColorChange;
    }

    public int getColor() {
        return this.color;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public void setColor(int i) {
        int r = ARGB.red(i);
        int g = ARGB.green(i);
        int b = ARGB.blue(i);

        float[] hsv = Color.RGBtoHSB(r, g, b, null);
        this.h = hsv[0];
        this.s = hsv[1];
        this.v = hsv[2];

        this.updateColor(Color.getHSBColor(this.h, this.s, this.v).getRGB());
    }

    public void setX(int i) {
        this.x = i;
    }

    public void setY(int i) {
        this.y = i;
    }

    public void setWidth(int i) {
        this.width = i;
    }

    public void setHeight(int i) {
        this.height = i;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent) {
        this.pickingColor = this.getWheelHovered(mouseButtonEvent.x(), mouseButtonEvent.y());
        this.pickingValue = this.getSliderHovered(mouseButtonEvent.x(), mouseButtonEvent.y());
        if (mouseButtonEvent.button() == 0 && this.getPicking()) {
            this.pickColorAt(mouseButtonEvent.x(), mouseButtonEvent.y());
            return true;
        }

        return false;
    }

    public boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double deltaX, double deltaY) {
        if (mouseButtonEvent.button() == 0 && this.getPicking()) {
            this.pickColorAt(mouseButtonEvent.x(), mouseButtonEvent.y());
            return true;
        }

        return false;
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        int wheelColor = Color.getHSBColor(this.h, this.s, 1.0F).getRGB();
        int wheelRadius = this.getWheelRadius();

        // render v picker
        int sliderX = this.getSliderX();
        int sliderY = this.getSliderY();
        guiGraphics.fillGradient(sliderX - (this.sliderWidth / 2), sliderY - wheelRadius, sliderX + (this.sliderWidth / 2), sliderY + wheelRadius, wheelColor, 0xFF000000);

        float vPickerX = sliderX;
        float vPickerY = sliderY - wheelRadius + ((wheelRadius * 2.0F) * (1.0F - this.v));
        VoxelMapGuiGraphics.blitFloat(guiGraphics, RenderPipelines.GUI_TEXTURED, this.verticalHandle, vPickerX - 8, vPickerY - 4, 16, 8, 0.0F, 1.0F, 0.0F, 1.0F, 0xFFFFFFFF);
        VoxelMapGuiGraphics.blitFloat(guiGraphics, RenderPipelines.GUI_TEXTURED, this.verticalHandleTint, vPickerX - 8, vPickerY - 4, 16, 8, 0.0F, 1.0F, 0.0F, 1.0F, this.color);

        // render h, s picker
        int wheelX = this.getWheelX();
        int wheelY = this.getWheelY();
        Identifier colorWheelImage = VoxelConstants.getVoxelMapInstance().getColorManager().getColorPicker();
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, colorWheelImage, wheelX - wheelRadius, wheelY - wheelRadius, 0.0F, 0.0F, wheelRadius * 2, wheelRadius * 2, wheelRadius * 2, wheelRadius * 2, 0xFFFFFFFF);

        float radians = this.h * 360.0F * Mth.DEG_TO_RAD;
        float dirX = Mth.cos(radians);
        float dirY = Mth.sin(radians);
        float distance = this.s * wheelRadius;
        float hsPickerX = wheelX + (dirX * distance);
        float hsPickerY = wheelY + (dirY * distance);
        VoxelMapGuiGraphics.blitFloat(guiGraphics, RenderPipelines.GUI_TEXTURED, this.roundHandle, hsPickerX - 4, hsPickerY - 4, 8, 8, 0.0F, 1.0F, 0.0F, 1.0F, 0xFFFFFFFF);
        VoxelMapGuiGraphics.blitFloat(guiGraphics, RenderPipelines.GUI_TEXTURED, this.roundHandleTint, hsPickerX - 4, hsPickerY - 4, 8, 8, 0.0F, 1.0F, 0.0F, 1.0F, wheelColor);

    }

    private void pickColorAt(double mouseX, double mouseY) {
        int wheelRadius = this.getWheelRadius();

        if (this.pickingValue) {
            // calculate v
            double dx = this.getSliderX() - mouseX;
            double dy = this.getSliderY() - mouseY;

            this.v = (float) Mth.clamp((dy + wheelRadius) / (wheelRadius * 2.0), 0.0, 1.0);
        }

        if (this.pickingColor) {
            // calculate h, s
            double dx = this.getWheelX() - mouseX;
            double dy = this.getWheelY() - mouseY;
            double degrees = Math.toDegrees(Math.atan2(dy, dx)) + 180.0F;
            double distance = Math.sqrt(dx * dx + dy * dy);

            this.h = (float) Mth.clamp((degrees / 360.0F), 0.0, 1.0);
            this.s = (float) Mth.clamp((distance / wheelRadius), 0.0, 1.0);
        }

        this.updateColor(Color.getHSBColor(this.h, this.s, this.v).getRGB());
    }

    private void updateColor(int color) {
        if (color != this.color) {
            this.color = color;
            this.onColorChange.onColorChange(this);
        }
    }

    private int getWheelRadius() {
        return this.height;
    }

    private int getWheelX() {
        return this.x - (this.width / 2) + this.getWheelRadius();
    }

    private int getWheelY() {
        return this.y;
    }

    private int getSliderX() {
        return this.x + (this.width / 2) - (this.sliderWidth / 2);
    }

    private int getSliderY() {
        return this.y;
    }

    private boolean getPicking() {
        return this.pickingColor || this.pickingValue;
    }

    private boolean getWheelHovered(double mouseX, double mouseY) {
        int wheelRadius = this.getWheelRadius();
        double dx = this.getWheelX() - mouseX;
        double dy = this.getWheelY() - mouseY;

        return (dx * dx + dy * dy) <= (wheelRadius * wheelRadius);
    }

    private boolean getSliderHovered(double mouseX, double mouseY) {
        int wheelRadius = this.getWheelRadius();
        double dx = this.getSliderX() - mouseX;
        double dy = this.getSliderY() - mouseY;

        return Math.abs(dx) <= (this.sliderWidth / 2.0) && Math.abs(dy) <= wheelRadius;
    }

    public interface OnColorChange {
        void onColorChange(GuiHSVColorPicker picker);
    }
}
