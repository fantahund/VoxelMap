package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.VoxelContants;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.GlAllocationUtils;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class GLUtils {
    private static final Tessellator tessellator = Tessellator.getInstance();
    private static final BufferBuilder vertexBuffer = tessellator.getBuffer();
    public static TextureManager textureManager;
    public static Framebuffer frameBuffer;
    public static int fboID = 0;
    public static int rboID = 0;
    public static int fboTextureID = 0;
    public static int depthTextureID = 0;
    private static int previousFBOID = 0;
    private static int previousFBOIDREAD = 0;
    private static int previousFBOIDDRAW = 0;
    public static final boolean hasAlphaBits = false;
    private static final IntBuffer dataBuffer = GlAllocationUtils.allocateByteBuffer(16777216).asIntBuffer();

    public static void setupFrameBuffer() {
        previousFBOID = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING); // unknown
        fboID = GL30.glGenFramebuffers();
        fboTextureID = GL11.glGenTextures();
        int width = 512;
        int height = 512;
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboID); // unknown
        ByteBuffer byteBuffer = BufferUtils.createByteBuffer(4 * width * height);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fboTextureID);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_BYTE, byteBuffer);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, fboTextureID, 0);
        rboID = GL30.glGenRenderbuffers();
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, rboID);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL14.GL_DEPTH_COMPONENT24, width, height);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER, rboID);
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, 0);
        checkFramebufferStatus();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFBOID);
        GlStateManager._bindTexture(0);
    }

    public static void setupFrameBufferUsingMinecraft() {
        frameBuffer = new SimpleFramebuffer(512, 512, true, VoxelContants.isSystemMacOS());
        fboID = frameBuffer.fbo;
        fboTextureID = frameBuffer.getColorAttachment();
    }

    public static void setupFrameBufferUsingMinecraftUnrolled() {
        RenderSystem.assertOnRenderThreadOrInit(); //Update 1.18
        fboID = GL30.glGenFramebuffers();
        fboTextureID = GL11.glGenTextures();
        depthTextureID = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthTextureID);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_MODE, 0);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_DEPTH_COMPONENT, 512, 512, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, (IntBuffer) null);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fboTextureID);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, 512, 512, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (IntBuffer) null);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboID);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, fboTextureID, 0);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, depthTextureID, 0);
        checkFramebufferStatus();
        GlStateManager._clearColor(1.0F, 1.0F, 1.0F, 0.0F);
        int i = 16384;
        GlStateManager._clearDepth(1.0);
        i |= 256;
        GlStateManager._clear(i, VoxelContants.isSystemMacOS());
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GlStateManager._bindTexture(0);
    }

    public static void checkFramebufferStatus() {
        int i = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (i != GL30.GL_FRAMEBUFFER_COMPLETE) {
            if (i == GL30.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT) {
                throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT");
            } else if (i == GL30.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT) {
                throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT");
            } else if (i == GL30.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER) {
                throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER");
            } else if (i == GL30.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER) {
                throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER");
            } else {
                throw new RuntimeException("glCheckFramebufferStatus returned unknown status:" + i);
            }
        }
    }

    public static void bindFrameBuffer() {
        previousFBOID = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        previousFBOIDREAD = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        previousFBOIDDRAW = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboID);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, fboID);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, fboID);
    }

    public static void unbindFrameBuffer() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFBOID);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousFBOIDREAD);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousFBOIDDRAW);
    }

    public static void setMap(int x, int y) {
        setMap((float) x, (float) y, 128);
    }

    public static void setMapWithScale(int x, int y, float scale) {
        setMap((float) x, (float) y, (int) (128.0F * scale));
    }

    public static void setMap(float x, float y, int imageSize) {
        float scale = (float) imageSize / 4.0F;
        ldrawthree(x - scale, y + scale, 1.0, 0.0F, 1.0F);
        ldrawthree(x + scale, y + scale, 1.0, 1.0F, 1.0F);
        ldrawthree(x + scale, y - scale, 1.0, 1.0F, 0.0F);
        ldrawthree(x - scale, y - scale, 1.0, 0.0F, 0.0F);
    }

    public static void setMap(Sprite icon, float x, float y, float imageSize) {
        float halfWidth = imageSize / 4.0F;
        ldrawthree(x - halfWidth, y + halfWidth, 1.0, icon.getMinU(), icon.getMaxV());
        ldrawthree(x + halfWidth, y + halfWidth, 1.0, icon.getMaxU(), icon.getMaxV());
        ldrawthree(x + halfWidth, y - halfWidth, 1.0, icon.getMaxU(), icon.getMinV());
        ldrawthree(x - halfWidth, y - halfWidth, 1.0, icon.getMinU(), icon.getMinV());
    }

    public static int tex(BufferedImage paramImg) {
        int glid = TextureUtil.generateTextureId();
        int width = paramImg.getWidth();
        int height = paramImg.getHeight();
        int[] imageData = new int[width * height];
        paramImg.getRGB(0, 0, width, height, imageData, 0, width);
        GLShim.glBindTexture(GL11.GL_TEXTURE_2D, glid);
        dataBuffer.clear();
        dataBuffer.put(imageData, 0, width * height);
        dataBuffer.position(0).limit(width * height);
        GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GLShim.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GLShim.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GLShim.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GLShim.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, dataBuffer);
        return glid;
    }

    public static void img(String paramStr) {
        textureManager.bindTexture(new Identifier(paramStr));
    }

    public static void img2(String paramStr) {
        RenderSystem.setShaderTexture(0, new Identifier(paramStr));
    }

    public static void img(Identifier paramResourceLocation) {
        textureManager.bindTexture(paramResourceLocation);
    }

    public static void img2(Identifier paramResourceLocation) {
        RenderSystem.setShaderTexture(0, paramResourceLocation);
    }

    public static void disp(int paramInt) {
        GLShim.glBindTexture(GL11.GL_TEXTURE_2D, paramInt);
    }

    public static void disp2(int paramInt) {
        RenderSystem.setShaderTexture(0, paramInt);
    }

    public static void register(Identifier resourceLocation, AbstractTexture image) {
        textureManager.registerTexture(resourceLocation, image);
    }

    public static NativeImage nativeImageFromBufferedImage(BufferedImage base) {
        int glid = tex(base);
        NativeImage nativeImage = new NativeImage(base.getWidth(), base.getHeight(), false);
        RenderSystem.bindTexture(glid);
        nativeImage.loadFromTextureImage(0, false);
        return nativeImage;
    }

    public static void drawPre() {
        vertexBuffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
    }

    public static void drawPre(VertexFormat vertexFormat) {
        vertexBuffer.begin(VertexFormat.DrawMode.QUADS, vertexFormat);
    }

    public static void drawPost() {
        tessellator.draw();
    }

    public static void glah(int g) {
        GLShim.glDeleteTextures(g);
    }

    public static void ldrawone(int x, int y, double z, float u, float v) {
        vertexBuffer.vertex(x, y, z).texture(u, v).next();
    }

    public static void ldrawtwo(double x, double y, double z) {
        vertexBuffer.vertex(x, y, z).next();
    }

    public static void ldrawthree(double x, double y, double z, float u, float v) {
        vertexBuffer.vertex(x, y, z).texture(u, v).next();
    }

    public static void ldrawthree(Matrix4f matrix4f, double x, double y, double z, float u, float v) {
        vertexBuffer.vertex(matrix4f, (float) x, (float) y, (float) z).texture(u, v).next();
    }
}
