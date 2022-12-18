package com.mamiyaotaru.voxelmap.gui.overridden;

import com.mamiyaotaru.voxelmap.VoxelContants;
import com.mamiyaotaru.voxelmap.util.GLShim;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

public abstract class GuiSlotMinimap extends EntryListWidget {
    protected int slotWidth = 220;
    private boolean showTopBottomBG = true;
    private boolean showSlotBG = true;
    private boolean hasListHeader;
    protected long lastClicked = 0L;
    public boolean doubleclick = false;

    public GuiSlotMinimap(int width, int height, int y1, int y2, int slotHeight) {
        super(VoxelContants.getMinecraft(), width, height, y1, y2, slotHeight);
        this.setZOffset(0);
    }

    public void setShowTopBottomBG(boolean showTopBottomBG) {
        this.showTopBottomBG = showTopBottomBG;
    }

    public void setShowSlotBG(boolean showSlotBG) {
        this.showSlotBG = showSlotBG;
    }

    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        int scrollBarLeft = this.getScrollbarPositionX();
        int scrollBarRight = scrollBarLeft + 6;
        this.setScrollAmount(this.getScrollAmount());
        GLShim.glDisable(2896);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder vertexBuffer = tessellator.getBuffer();
        if (this.showSlotBG) {
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, Screen.OPTIONS_BACKGROUND_TEXTURE);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            float f = 32.0F;
            vertexBuffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            vertexBuffer.vertex(this.left, this.bottom, 0.0).texture((float) this.left / f, (float) (this.bottom + (int) this.getScrollAmount()) / f).color(32, 32, 32, 255).next();
            vertexBuffer.vertex(this.right, this.bottom, 0.0).texture((float) this.right / f, (float) (this.bottom + (int) this.getScrollAmount()) / f).color(32, 32, 32, 255).next();
            vertexBuffer.vertex(this.right, this.top, 0.0).texture((float) this.right / f, (float) (this.top + (int) this.getScrollAmount()) / f).color(32, 32, 32, 255).next();
            vertexBuffer.vertex(this.left, this.top, 0.0).texture((float) this.left / f, (float) (this.top + (int) this.getScrollAmount()) / f).color(32, 32, 32, 255).next();
            tessellator.draw();
        }

        int leftEdge = this.left + this.width / 2 - this.getRowWidth() / 2 + 2;
        int topOfListYPos = this.top + 4 - (int) this.getScrollAmount();
        if (this.hasListHeader) {
            this.renderHeader(matrixStack, leftEdge, topOfListYPos, tessellator);
        }

        this.renderList(matrixStack, mouseX, mouseY, partialTicks);
        GLShim.glDisable(2929);
        byte topBottomFadeHeight = 4;
        if (this.showTopBottomBG) {
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, Screen.OPTIONS_BACKGROUND_TEXTURE);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(519);
            vertexBuffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            vertexBuffer.vertex(this.left, this.top, -100.0).texture(0.0F, (float) this.top / 32.0F).color(64, 64, 64, 255).next();
            vertexBuffer.vertex(this.left + this.width, this.top, -100.0).texture((float) this.width / 32.0F, (float) this.top / 32.0F).color(64, 64, 64, 255).next();
            vertexBuffer.vertex(this.left + this.width, 0.0, -100.0).texture((float) this.width / 32.0F, 0.0F).color(64, 64, 64, 255).next();
            vertexBuffer.vertex(this.left, 0.0, -100.0).texture(0.0F, 0.0F).color(64, 64, 64, 255).next();
            vertexBuffer.vertex(this.left, this.height, -100.0).texture(0.0F, (float) this.height / 32.0F).color(64, 64, 64, 255).next();
            vertexBuffer.vertex(this.left + this.width, this.height, -100.0).texture((float) this.width / 32.0F, (float) this.height / 32.0F).color(64, 64, 64, 255).next();
            vertexBuffer.vertex(this.left + this.width, this.bottom, -100.0).texture((float) this.width / 32.0F, (float) this.bottom / 32.0F).color(64, 64, 64, 255).next();
            vertexBuffer.vertex(this.left, this.bottom, -100.0).texture(0.0F, (float) this.bottom / 32.0F).color(64, 64, 64, 255).next();
            tessellator.draw();
            RenderSystem.depthFunc(515);
            RenderSystem.disableDepthTest();
            GLShim.glEnable(3042);
            RenderSystem.blendFuncSeparate(770, 771, 0, 1);
            GLShim.glDisable(3553);
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, OPTIONS_BACKGROUND_TEXTURE);
            vertexBuffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            vertexBuffer.vertex(this.left, this.top + topBottomFadeHeight, 0.0).texture(0.0F, 1.0F).color(0, 0, 0, 0).next();
            vertexBuffer.vertex(this.right, this.top + topBottomFadeHeight, 0.0).texture(1.0F, 1.0F).color(0, 0, 0, 0).next();
            vertexBuffer.vertex(this.right, this.top, 0.0).texture(1.0F, 0.0F).color(0, 0, 0, 255).next();
            vertexBuffer.vertex(this.left, this.top, 0.0).texture(0.0F, 0.0F).color(0, 0, 0, 255).next();
            vertexBuffer.vertex(this.left, this.bottom, 0.0).texture(0.0F, 1.0F).color(0, 0, 0, 255).next();
            vertexBuffer.vertex(this.right, this.bottom, 0.0).texture(1.0F, 1.0F).color(0, 0, 0, 255).next();
            vertexBuffer.vertex(this.right, this.bottom - topBottomFadeHeight, 0.0).texture(1.0F, 0.0F).color(0, 0, 0, 0).next();
            vertexBuffer.vertex(this.left, this.bottom - topBottomFadeHeight, 0.0).texture(0.0F, 0.0F).color(0, 0, 0, 0).next();
            tessellator.draw();
        }

        int maxScroll = this.getMaxScroll();
        if (maxScroll > 0) {
            GLShim.glDisable(3553);
            RenderSystem.disableTexture();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            int k1 = (this.bottom - this.top) * (this.bottom - this.top) / this.getMaxPosition();
            k1 = MathHelper.clamp(k1, 32, this.bottom - this.top - 8);
            int l1 = (int) this.getScrollAmount() * (this.bottom - this.top - k1) / maxScroll + this.top;
            if (l1 < this.top) {
                l1 = this.top;
            }

            vertexBuffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            vertexBuffer.vertex(scrollBarLeft, this.bottom, 0.0).color(0, 0, 0, 255).next();
            vertexBuffer.vertex(scrollBarRight, this.bottom, 0.0).color(0, 0, 0, 255).next();
            vertexBuffer.vertex(scrollBarRight, this.top, 0.0).color(0, 0, 0, 255).next();
            vertexBuffer.vertex(scrollBarLeft, this.top, 0.0).color(0, 0, 0, 255).next();
            vertexBuffer.vertex(scrollBarLeft, l1 + k1, 0.0).color(128, 128, 128, 255).next();
            vertexBuffer.vertex(scrollBarRight, l1 + k1, 0.0).color(128, 128, 128, 255).next();
            vertexBuffer.vertex(scrollBarRight, l1, 0.0).color(128, 128, 128, 255).next();
            vertexBuffer.vertex(scrollBarLeft, l1, 0.0).color(128, 128, 128, 255).next();
            vertexBuffer.vertex(scrollBarLeft, l1 + k1 - 1, 0.0).color(192, 192, 192, 255).next();
            vertexBuffer.vertex(scrollBarRight - 1, l1 + k1 - 1, 0.0).color(192, 192, 192, 255).next();
            vertexBuffer.vertex(scrollBarRight - 1, l1, 0.0).color(192, 192, 192, 255).next();
            vertexBuffer.vertex(scrollBarLeft, l1, 0.0).color(192, 192, 192, 255).next();
            tessellator.draw();
        }

        this.renderDecorations(matrixStack, mouseX, mouseY);
        GLShim.glEnable(3553);
        GLShim.glDisable(3042);
    }

    public int getRowWidth() {
        return this.slotWidth;
    }

    public void setSlotWidth(int slotWidth) {
        this.slotWidth = slotWidth;
    }

    protected int getScrollbarPositionX() {
        return this.slotWidth >= 220 ? this.width / 2 + 124 : this.right - 6;
    }

    public void setLeftPos(int x1) {
        this.left = x1;
        this.right = x1 + this.width;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        this.doubleclick = System.currentTimeMillis() - this.lastClicked < 250L;
        this.lastClicked = System.currentTimeMillis();
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    public void appendNarrations(NarrationMessageBuilder builder) {
    }
}
