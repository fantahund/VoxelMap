package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
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
    private static final int previousProgram = 0;
    public static boolean hasAlphaBits = false;// Suppress error, the codepath that uses this makes no sense.
    //public static boolean hasAlphaBits = GL30.glGetFramebufferAttachmentParameteri(36008, 1026, 33301) > 0;
    public static final int fboSize = 512;
    public static final int fboRad = 256;
    private static final IntBuffer dataBuffer = GlAllocationUtils.allocateByteBuffer(16777216).asIntBuffer();

    public static void setupFrameBuffer() {
        previousFBOID = GL11.glGetInteger(36006);
        fboID = GL30.glGenFramebuffers();
        fboTextureID = GL11.glGenTextures();
        int width = 512;
        int height = 512;
        GL30.glBindFramebuffer(36160, fboID);
        ByteBuffer byteBuffer = BufferUtils.createByteBuffer(4 * width * height);
        GL11.glBindTexture(3553, fboTextureID);
        GL11.glTexParameteri(3553, 10242, 10496);
        GL11.glTexParameteri(3553, 10243, 10496);
        GL11.glTexParameteri(3553, 10241, 9729);
        GL11.glTexParameteri(3553, 10240, 9729);
        GL11.glTexImage2D(3553, 0, 6408, width, height, 0, 6408, 5120, byteBuffer);
        GL30.glFramebufferTexture2D(36160, 36064, 3553, fboTextureID, 0);
        rboID = GL30.glGenRenderbuffers();
        GL30.glBindRenderbuffer(36161, rboID);
        GL30.glRenderbufferStorage(36161, 33190, width, height);
        GL30.glFramebufferRenderbuffer(36160, 36096, 36161, rboID);
        GL30.glBindRenderbuffer(36161, 0);
        checkFramebufferStatus();
        GL30.glBindFramebuffer(36160, previousFBOID);
        GlStateManager._bindTexture(0);
    }

    public static void setupFrameBufferUsingMinecraft() {
        frameBuffer = new SimpleFramebuffer(512, 512, true, MinecraftClient.IS_SYSTEM_MAC);
        fboID = frameBuffer.fbo;
        fboTextureID = frameBuffer.getColorAttachment();
    }

    public static void setupFrameBufferUsingMinecraftUnrolled() {
        RenderSystem.assertOnRenderThreadOrInit(); //Update 1.18
        fboID = GL30.glGenFramebuffers();
        fboTextureID = GL11.glGenTextures();
        depthTextureID = GL11.glGenTextures();
        GL11.glBindTexture(3553, depthTextureID);
        GL11.glTexParameteri(3553, 10241, 9728);
        GL11.glTexParameteri(3553, 10240, 9728);
        GL11.glTexParameteri(3553, 34892, 0);
        GL11.glTexImage2D(3553, 0, 6402, 512, 512, 0, 6402, 5126, (IntBuffer) null);
        GL11.glBindTexture(3553, fboTextureID);
        GL11.glTexParameteri(3553, 10241, 9729);
        GL11.glTexParameteri(3553, 10240, 9729);
        GL11.glTexImage2D(3553, 0, 32856, 512, 512, 0, 6408, 5121, (IntBuffer) null);
        GL30.glBindFramebuffer(36160, fboID);
        GL30.glFramebufferTexture2D(36160, 36064, 3553, fboTextureID, 0);
        GL30.glFramebufferTexture2D(36160, 36096, 3553, depthTextureID, 0);
        checkFramebufferStatus();
        GlStateManager._clearColor(1.0F, 1.0F, 1.0F, 0.0F);
        int i = 16384;
        GlStateManager._clearDepth(1.0);
        i |= 256;
        GlStateManager._clear(i, MinecraftClient.IS_SYSTEM_MAC);
        GlStateManager._glBindFramebuffer(36160, 0);
        GlStateManager._bindTexture(0);
    }

    public static void checkFramebufferStatus() {
        int i = GL30.glCheckFramebufferStatus(36160);
        if (i != 36053) {
            if (i == 36054) {
                throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT");
            } else if (i == 36055) {
                throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT");
            } else if (i == 36059) {
                throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER");
            } else if (i == 36060) {
                throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER");
            } else {
                throw new RuntimeException("glCheckFramebufferStatus returned unknown status:" + i);
            }
        }
    }

    public static void bindFrameBuffer() {
        previousFBOID = GL11.glGetInteger(36006);
        previousFBOIDREAD = GL11.glGetInteger(36010);
        previousFBOIDDRAW = GL11.glGetInteger(36006);
        GL30.glBindFramebuffer(36160, fboID);
        GL30.glBindFramebuffer(36008, fboID);
        GL30.glBindFramebuffer(36009, fboID);
    }

    public static void unbindFrameBuffer() {
        GL30.glBindFramebuffer(36160, previousFBOID);
        GL30.glBindFramebuffer(36008, previousFBOIDREAD);
        GL30.glBindFramebuffer(36009, previousFBOIDDRAW);
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
        GLShim.glBindTexture(3553, glid);
        dataBuffer.clear();
        dataBuffer.put(imageData, 0, width * height);
        dataBuffer.position(0).limit(width * height);
        GLShim.glTexParameteri(3553, 10241, 9729);
        GLShim.glTexParameteri(3553, 10240, 9729);
        GLShim.glPixelStorei(3314, 0);
        GLShim.glPixelStorei(3316, 0);
        GLShim.glPixelStorei(3315, 0);
        GLShim.glTexImage2D(3553, 0, 6408, width, height, 0, 32993, 33639, dataBuffer);
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
        GLShim.glBindTexture(3553, paramInt);
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
