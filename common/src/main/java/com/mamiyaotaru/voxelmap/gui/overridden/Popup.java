package com.mamiyaotaru.voxelmap.gui.overridden;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.util.VoxelmapGuiGraphics;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

public class Popup {
    private final Font fontRendererObj;
    private int x;
    private int y;
    private final PopupEntry[] entries;
    private int w;
    private final int h;
    private final int clickedDirectX;
    private final int clickedDirectY;
    private boolean shouldClose;
    private final PopupGuiScreen parentGui;
    private final int padding = 6;

    public Popup(int x, int y, int directX, int directY, ArrayList<PopupEntry> entries, PopupGuiScreen parentGui) {
        this.fontRendererObj = VoxelConstants.getMinecraft().font;
        this.parentGui = parentGui;
        this.clickedDirectX = directX;
        this.clickedDirectY = directY;
        this.x = x - 1;
        this.y = y - 1;
        this.entries = new PopupEntry[entries.size()];
        entries.toArray(this.entries);
        this.w = 0;
        this.h = this.entries.length * 20;

        for (PopupEntry entry : this.entries) {
            int entryWidth = this.fontRendererObj.width(entry.name);
            if (entryWidth > this.w) {
                this.w = entryWidth;
            }
        }

        this.w += this.padding * 2;
        if (x + this.w > parentGui.width) {
            this.x = x - this.w + 2;
        }

        if (y + this.h > parentGui.height) {
            this.y = y - this.h + 2;
        }

    }

    public int getClickedDirectX() {
        return clickedDirectX;
    }

    public int getClickedDirectY() {
        return clickedDirectY;
    }

    public boolean clickedMe(double mouseX, double mouseY) {
        boolean clicked = mouseX > this.x && mouseX < (this.x + this.w) && mouseY > this.y && mouseY < (this.y + this.h);
        if (clicked) {
            for (int t = 0; t < this.entries.length; ++t) {
                if (this.entries[t].enabled) {
                    boolean entryClicked = mouseX >= this.x && mouseX <= (this.x + this.w) && mouseY >= (this.y + t * 20) && mouseY <= (this.y + (t + 1) * 20);
                    if (entryClicked) {
                        this.shouldClose = this.entries[t].causesClose;
                        this.parentGui.popupAction(this, this.entries[t].action);
                    }
                }
            }
        }

        return clicked;
    }

    public boolean overMe(int x, int y) {
        return x > this.x && x < this.x + this.w && y > this.y && y < this.y + this.h;
    }

    public boolean shouldClose() {
        return this.shouldClose;
    }

    public void drawPopup(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.pose().pushMatrix();
        // FIXME 1.21.6 z-order guiGraphics.pose().translate(0, 0, 10);

        // // background
        float renderedTextureSize = 32.0F;
        float umin = this.x / renderedTextureSize;
        float umax = (this.x + this.w) / renderedTextureSize;
        float vmin = this.y / renderedTextureSize;
        float vmax = (this.y + this.h) / renderedTextureSize;
        VoxelmapGuiGraphics.blitFloat(guiGraphics, RenderPipelines.GUI_TEXTURED, VoxelConstants.getOptionsBackgroundTexture(), x, y, w, h, umin, umax, vmin, vmax, 0xffffffff);
        VoxelmapGuiGraphics.fillGradient(guiGraphics, x, y, x + w, y + 4, 0xff000000, 0xff000000, 0x00000000, 0x00000000);
        VoxelmapGuiGraphics.fillGradient(guiGraphics, x, y + h - 4, x + w, y + h, 0x00000000, 0x00000000, 0xff000000, 0xff000000);

        VoxelmapGuiGraphics.fillGradient(guiGraphics, x, y, x + 4, y + h, 0xff000000, 0x00000000, 0xff000000, 0x00000000);
        VoxelmapGuiGraphics.fillGradient(guiGraphics, x + w - 4, y, x + w, y + h, 0x00000000, 0xff000000, 0x00000000, 0xff000000);


        for (int t = 0; t < this.entries.length; ++t) {
            int color = !this.entries[t].enabled ? 10526880 : (mouseX >= this.x && mouseX <= this.x + this.w && mouseY >= this.y + t * 20 && mouseY <= this.y + (t + 1) * 20 ? 16777120 : 14737632);
            guiGraphics.drawString(this.fontRendererObj, this.entries[t].name, (this.x + this.padding), (this.y + this.padding + t * 20), color);
        }
        guiGraphics.pose().popMatrix();
    }

    public static class PopupEntry {
        protected final String name;
        protected final int action;
        protected final boolean causesClose;
        protected final boolean enabled;

        public PopupEntry(String name, int action, boolean causesClose, boolean enabled) {
            this.name = name;
            this.action = action;
            this.causesClose = causesClose;
            this.enabled = enabled;
        }
    }
}
