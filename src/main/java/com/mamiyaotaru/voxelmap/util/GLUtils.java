package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.GlAllocationUtils;
import net.minecraft.util.Identifier;
import org.lwjgl.BufferUtils;

import java.awt.image.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

@Deprecated
public class GLUtils {
    private static final Tessellator tessellator = Tessellator.getInstance();
    private static final BufferBuilder vertexBuffer = tessellator.getBuffer();
    public static TextureManager textureManager;
    public static int fboID;
    public static int rboID;
    public static int fboTextureID;
    private static int previousFBOID;
    private static int previousFBOIDREAD;
    private static int previousFBOIDDRAW;
    private static final IntBuffer dataBuffer = GlAllocationUtils.allocateByteBuffer(16777216).asIntBuffer();

    @Deprecated
    public static void setupFrameBuffer() {
        previousFBOID = OpenGL.glGetInteger(OpenGL.GL30_GL_FRAMEBUFFER_BINDING); // unknown
        fboID = OpenGL.glGenFramebuffers();
        fboTextureID = OpenGL.glGenTextures();
        int width = 512;
        int height = 512;
        OpenGL.glBindFramebuffer(OpenGL.GL30_GL_FRAMEBUFFER, fboID); // unknown
        ByteBuffer byteBuffer = BufferUtils.createByteBuffer(4 * width * height);
        OpenGL.glBindTexture(OpenGL.GL11_GL_TEXTURE_2D, fboTextureID);
        OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_WRAP_S, OpenGL.GL11_GL_CLAMP);
        OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_WRAP_T, OpenGL.GL11_GL_CLAMP);
        OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MIN_FILTER, OpenGL.GL11_GL_LINEAR);
        OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MAG_FILTER, OpenGL.GL11_GL_LINEAR);
        OpenGL.glTexImage2D(OpenGL.GL11_GL_TEXTURE_2D, 0, OpenGL.GL11_GL_RGBA, width, height, 0, OpenGL.GL11_GL_RGBA, OpenGL.GL11_GL_BYTE, byteBuffer);
        OpenGL.glFramebufferTexture2D(OpenGL.GL30_GL_FRAMEBUFFER, OpenGL.GL30_GL_COLOR_ATTACHMENT0, OpenGL.GL11_GL_TEXTURE_2D, fboTextureID, 0);
        rboID = OpenGL.glGenRenderbuffers();
        OpenGL.glBindRenderbuffer(OpenGL.GL30_GL_RENDERBUFFER, rboID);
        OpenGL.glRenderbufferStorage(OpenGL.GL30_GL_RENDERBUFFER, OpenGL.GL14_GL_DEPTH_COMPONENT24, width, height);
        OpenGL.glFramebufferRenderbuffer(OpenGL.GL30_GL_FRAMEBUFFER, OpenGL.GL30_GL_DEPTH_ATTACHMENT, OpenGL.GL30_GL_RENDERBUFFER, rboID);
        OpenGL.glBindRenderbuffer(OpenGL.GL30_GL_RENDERBUFFER, 0);
        checkFramebufferStatus();
        OpenGL.glBindFramebuffer(OpenGL.GL30_GL_FRAMEBUFFER, previousFBOID);
        GlStateManager._bindTexture(0);
    }

    @Deprecated
    public static void checkFramebufferStatus() {
        int i = OpenGL.glCheckFramebufferStatus(OpenGL.GL30_GL_FRAMEBUFFER);
        if (i != OpenGL.GL30_GL_FRAMEBUFFER_COMPLETE) {
            if (i == OpenGL.GL30_GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT) {
                throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT");
            } else if (i == OpenGL.GL30_GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT) {
                throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT");
            } else if (i == OpenGL.GL30_GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER) {
                throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER");
            } else if (i == OpenGL.GL30_GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER) {
                throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER");
            } else {
                throw new RuntimeException("glCheckFramebufferStatus returned unknown status:" + i);
            }
        }
    }

    @Deprecated
    public static void bindFrameBuffer() {
        previousFBOID = OpenGL.glGetInteger(OpenGL.GL30_GL_FRAMEBUFFER_BINDING);
        previousFBOIDREAD = OpenGL.glGetInteger(OpenGL.GL30_GL_READ_FRAMEBUFFER_BINDING);
        previousFBOIDDRAW = OpenGL.glGetInteger(OpenGL.GL30_GL_FRAMEBUFFER_BINDING);
        OpenGL.glBindFramebuffer(OpenGL.GL30_GL_FRAMEBUFFER, fboID);
        OpenGL.glBindFramebuffer(OpenGL.GL30_GL_READ_FRAMEBUFFER, fboID);
        OpenGL.glBindFramebuffer(OpenGL.GL30_GL_DRAW_FRAMEBUFFER, fboID);
    }

    @Deprecated
    public static void unbindFrameBuffer() {
        OpenGL.glBindFramebuffer(OpenGL.GL30_GL_FRAMEBUFFER, previousFBOID);
        OpenGL.glBindFramebuffer(OpenGL.GL30_GL_READ_FRAMEBUFFER, previousFBOIDREAD);
        OpenGL.glBindFramebuffer(OpenGL.GL30_GL_DRAW_FRAMEBUFFER, previousFBOIDDRAW);
    }

    @Deprecated
    public static void setMapWithScale(int x, int y, float scale) {
        setMap(x, y, (int) (128.0F * scale));
    }

    @Deprecated
    public static void setMap(float x, float y, int imageSize) {
        float scale = imageSize / 4.0F;
        ldrawthree(x - scale, y + scale, 1.0, 0.0F, 1.0F);
        ldrawthree(x + scale, y + scale, 1.0, 1.0F, 1.0F);
        ldrawthree(x + scale, y - scale, 1.0, 1.0F, 0.0F);
        ldrawthree(x - scale, y - scale, 1.0, 0.0F, 0.0F);
    }

    @Deprecated
    public static void setMap(Sprite icon, float x, float y, float imageSize) {
        float halfWidth = imageSize / 4.0F;
        ldrawthree(x - halfWidth, y + halfWidth, 1.0, icon.getMinU(), icon.getMaxV());
        ldrawthree(x + halfWidth, y + halfWidth, 1.0, icon.getMaxU(), icon.getMaxV());
        ldrawthree(x + halfWidth, y - halfWidth, 1.0, icon.getMaxU(), icon.getMinV());
        ldrawthree(x - halfWidth, y - halfWidth, 1.0, icon.getMinU(), icon.getMinV());
    }

    @Deprecated
    public static int tex(BufferedImage paramImg) {
        int glid = TextureUtil.generateTextureId();
        int width = paramImg.getWidth();
        int height = paramImg.getHeight();
        int[] imageData = new int[width * height];
        paramImg.getRGB(0, 0, width, height, imageData, 0, width);
        OpenGL.glBindTexture(OpenGL.GL11_GL_TEXTURE_2D, glid);
        dataBuffer.clear();
        dataBuffer.put(imageData, 0, width * height);
        dataBuffer.position(0).limit(width * height);
        OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MIN_FILTER, OpenGL.GL11_GL_LINEAR);
        OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MAG_FILTER, OpenGL.GL11_GL_LINEAR);
        OpenGL.glPixelStorei(OpenGL.GL11_GL_UNPACK_ROW_LENGTH, 0);
        OpenGL.glPixelStorei(OpenGL.GL11_GL_UNPACK_SKIP_PIXELS, 0);
        OpenGL.glPixelStorei(OpenGL.GL11_GL_UNPACK_SKIP_ROWS, 0);
        OpenGL.glTexImage2D(OpenGL.GL11_GL_TEXTURE_2D, 0, OpenGL.GL11_GL_RGBA, width, height, 0, OpenGL.GL12_GL_BGRA, OpenGL.GL12_GL_UNSIGNED_INT_8_8_8_8_REV, dataBuffer);
        return glid;
    }

    @Deprecated
    public static void img2(String paramStr) {
        RenderSystem.setShaderTexture(0, new Identifier(paramStr));
    }

    @Deprecated
    public static void img(Identifier paramResourceLocation) {
        textureManager.bindTexture(paramResourceLocation);
    }

    @Deprecated
    public static void img2(Identifier paramResourceLocation) {
        RenderSystem.setShaderTexture(0, paramResourceLocation);
    }

    @Deprecated
    public static void disp(int paramInt) {
        OpenGL.glBindTexture(OpenGL.GL11_GL_TEXTURE_2D, paramInt);
    }

    @Deprecated
    public static void disp2(int paramInt) {
        RenderSystem.setShaderTexture(0, paramInt);
    }

    @Deprecated
    public static void register(Identifier resourceLocation, AbstractTexture image) {
        textureManager.registerTexture(resourceLocation, image);
    }

    @Deprecated
    public static NativeImage nativeImageFromBufferedImage(BufferedImage base) {
        int glid = tex(base);
        NativeImage nativeImage = new NativeImage(base.getWidth(), base.getHeight(), false);
        RenderSystem.bindTexture(glid);
        nativeImage.loadFromTextureImage(0, false);
        return nativeImage;
    }

    @Deprecated
    public static void drawPre() {
        vertexBuffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
    }

    @Deprecated
    public static void drawPre(VertexFormat vertexFormat) {
        vertexBuffer.begin(VertexFormat.DrawMode.QUADS, vertexFormat);
    }

    @Deprecated
    public static void drawPost() {
        tessellator.draw();
    }

    @Deprecated
    public static void glah(int g) {
        OpenGL.glDeleteTexture(g);
    }

    @Deprecated
    public static void ldrawone(int x, int y, double z, float u, float v) {
        vertexBuffer.vertex(x, y, z).texture(u, v).next();
    }

    @Deprecated
    public static void ldrawtwo(double x, double y, double z) {
        vertexBuffer.vertex(x, y, z).next();
    }

    @Deprecated
    public static void ldrawthree(double x, double y, double z, float u, float v) {
        vertexBuffer.vertex(x, y, z).texture(u, v).next();
    }
}