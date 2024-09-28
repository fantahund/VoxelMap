package com.mamiyaotaru.voxelmap.gui.overridden;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.util.OpenGL;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;

public class Popup {
    final Font fontRendererObj;
    int x;
    int y;
    final PopupEntry[] entries;
    int w;
    final int h;
    public final int clickedX;
    public final int clickedY;
    public final int clickedDirectX;
    public final int clickedDirectY;
    boolean shouldClose;
    final PopupGuiScreen parentGui;
    final int padding = 6;

    public Popup(int x, int y, int directX, int directY, ArrayList<PopupEntry> entries, PopupGuiScreen parentGui) {
        this.fontRendererObj = VoxelConstants.getMinecraft().font;
        this.parentGui = parentGui;
        this.clickedX = x;
        this.clickedY = y;
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

    public void drawPopup(GuiGraphics drawContext, int mouseX, int mouseY) {
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder vertexBuffer = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        OpenGL.glDisable(OpenGL.GL11_GL_DEPTH_TEST);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, VoxelConstants.getOptionsBackgroundTexture());
        OpenGL.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        float var6 = 32.0F;
        vertexBuffer.addVertex(this.x, this.y + this.h, 0.0F).setUv(this.x / var6, this.y / var6).setColor(64, 64, 64, 255);
        vertexBuffer.addVertex(this.x + this.w, this.y + this.h, 0.0F).setUv((this.x + this.w) / var6, this.y / var6).setColor(64, 64, 64, 255);
        vertexBuffer.addVertex(this.x + this.w, this.y, 0.0F).setUv((this.x + this.w) / var6, (this.y + this.h) / var6).setColor(64, 64, 64, 255);
        vertexBuffer.addVertex(this.x, this.y, 0.0F).setUv(this.x / var6, (this.y + this.h) / var6).setColor(64, 64, 64, 255);
        BufferUploader.drawWithShader(vertexBuffer.buildOrThrow());
        OpenGL.glEnable(OpenGL.GL11_GL_BLEND);
        OpenGL.glBlendFunc(OpenGL.GL11_GL_SRC_ALPHA, OpenGL.GL11_GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        vertexBuffer = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        vertexBuffer.addVertex(this.x, this.y + 4, 0.0F).setColor(0, 0, 0, 0);
        vertexBuffer.addVertex(this.x + this.w, this.y + 4, 0.0F).setColor(0, 0, 0, 0);
        vertexBuffer.addVertex(this.x + this.w, this.y, 0.0F).setColor(0, 0, 0, 255);
        vertexBuffer.addVertex(this.x, this.y, 0.0F).setColor(0, 0, 0, 255);
        vertexBuffer.addVertex(this.x, this.y + this.h, 0.0F).setColor(0, 0, 0, 255);
        vertexBuffer.addVertex(this.x + this.w, this.y + this.h, 0.0F).setColor(0, 0, 0, 255);
        vertexBuffer.addVertex(this.x + this.w, this.y + this.h - 4, 0.0F).setColor(0, 0, 0, 0);
        vertexBuffer.addVertex(this.x, this.y + this.h - 4, 0.0F).setColor(0, 0, 0, 0);
        vertexBuffer.addVertex(this.x, this.y, 0.0F).setColor(0, 0, 0, 255);
        vertexBuffer.addVertex(this.x, this.y + this.h, 0.0F).setColor(0, 0, 0, 255);
        vertexBuffer.addVertex(this.x + 4, this.y + this.h, 0.0F).setColor(0, 0, 0, 0);
        vertexBuffer.addVertex(this.x + 4, this.y, 0.0F).setColor(0, 0, 0, 0);
        vertexBuffer.addVertex(this.x + this.w - 4, this.y, 0.0F).setColor(0, 0, 0, 0);
        vertexBuffer.addVertex(this.x + this.w - 4, this.y + this.h, 0.0F).setColor(0, 0, 0, 0);
        vertexBuffer.addVertex(this.x + this.w, this.y + this.h, 0.0F).setColor(0, 0, 0, 255);
        vertexBuffer.addVertex(this.x + this.w, this.y, 0.0F).setColor(0, 0, 0, 255);
        BufferUploader.drawWithShader(vertexBuffer.buildOrThrow());
        vertexBuffer = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        vertexBuffer.addVertex(this.x + this.w - 4, this.y, 0.0F).setColor(0, 0, 0, 0);
        vertexBuffer.addVertex(this.x + this.w - 4, this.y + this.h, 0.0F).setColor(0, 0, 0, 0);
        vertexBuffer.addVertex(this.x + this.w, this.y + this.h, 0.0F).setColor(0, 0, 0, 255);
        vertexBuffer.addVertex(this.x + this.w, this.y, 0.0F).setColor(0, 0, 0, 255);
        BufferUploader.drawWithShader(vertexBuffer.buildOrThrow());
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        OpenGL.glDisable(OpenGL.GL11_GL_BLEND);

        for (int t = 0; t < this.entries.length; ++t) {
            int color = !this.entries[t].enabled ? 10526880 : (mouseX >= this.x && mouseX <= this.x + this.w && mouseY >= this.y + t * 20 && mouseY <= this.y + (t + 1) * 20 ? 16777120 : 14737632);
            drawContext.drawString(this.fontRendererObj, this.entries[t].name, (this.x + this.padding), (this.y + this.padding + t * 20), color);
        }

    }

    public static class PopupEntry {
        public final String name;
        public final int action;
        final boolean causesClose;
        final boolean enabled;

        public PopupEntry(String name, int action, boolean causesClose, boolean enabled) {
            this.name = name;
            this.action = action;
            this.causesClose = causesClose;
            this.enabled = enabled;
        }
    }
}
