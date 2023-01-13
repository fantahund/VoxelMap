package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public final class OpenGL {
    public static final int
            GL11_GL_DEPTH_BUFFER_BIT     = 0x100,
            GL11_GL_LEQUAL               = 0x203,
            GL11_GL_ALWAYS               = 0x207,
            GL11_GL_SRC_ALPHA            = 0x302,
            GL11_GL_ONE_MINUS_SRC_ALPHA  = 0x303,
            GL11_GL_CULL_FACE            = 0xB44,
            GL11_GL_LIGHTING             = 0xB50,
            GL11_GL_DEPTH_TEST           = 0xB71,
            GL11_GL_BLEND                = 0xBE2,
            GL11_GL_SCISSOR_TEST         = 0xC11,
            GL11_GL_COLOR_CLEAR_VALUE    = 0xC22,
            GL11_GL_UNPACK_ROW_LENGTH    = 0xCF2,
            GL11_GL_UNPACK_SKIP_ROWS     = 0xCF3,
            GL11_GL_UNPACK_SKIP_PIXELS   = 0xCF4,
            GL11_GL_UNPACK_ALIGNMENT     = 0xCF5,
            GL11_GL_PACK_ALIGNMENT       = 0xD05,
            GL11_GL_TEXTURE_2D           = 0xDE1,
            GL11_GL_TRANSFORM_BIT        = 0x1000,
            GL11_GL_TEXTURE_HEIGHT       = 0x1001,
            GL11_GL_UNSIGNED_BYTE        = 0x1401,
            GL11_GL_RGBA                 = 0x1908,
            GL11_GL_NEAREST              = 0x2600,
            GL11_GL_LINEAR               = 0x2601,
            GL11_GL_LINEAR_MIPMAP_LINEAR = 0x2703,
            GL11_GL_TEXTURE_MAG_FILTER   = 0x2800,
            GL11_GL_TEXTURE_MIN_FILTER   = 0x2801,
            GL11_GL_TEXTURE_WRAP_S       = 0x2802,
            GL11_GL_TEXTURE_WRAP_T       = 0x2803,
            GL11_GL_CLAMP                = 0x2900,
            GL11_GL_COLOR_BUFFER_BIT     = 0x4000,
            GL11_GL_POLYGON_OFFSET_FILL  = 0x8037,
            GL11_GL_TEXTURE_BINDING_2D   = 0x8069;

    public static final int
            GL12_GL_UNSIGNED_INT_8_8_8_8     = 0x8035,
            GL12_GL_BGRA                     = 0x80E1,
            GL12_GL_CLAMP_TO_EDGE            = 0x812F,
            GL12_GL_UNSIGNED_INT_8_8_8_8_REV = 0x8367;

    public static final int
            GL30_GL_READ_FRAMEBUFFER         = 0x8CA8,
            GL30_GL_DRAW_FRAMEBUFFER         = 0x8CA9;

    private OpenGL() {}

    public static void glEnable(int target) {
        switch (target) {
            case GL11_GL_CULL_FACE -> RenderSystem.enableCull();
            case GL11_GL_DEPTH_TEST -> RenderSystem.enableDepthTest();
            case GL11_GL_BLEND -> RenderSystem.enableBlend();
            case GL11_GL_SCISSOR_TEST -> GL11.glEnable(GL11_GL_SCISSOR_TEST);
            case GL11_GL_TEXTURE_2D -> RenderSystem.enableTexture();
            case GL11_GL_POLYGON_OFFSET_FILL -> RenderSystem.enablePolygonOffset();
            default -> VoxelConstants.getLogger().warn("OpenGL - Invalid state received");
        }
    }

    public static void glDisable(int target) {
        switch (target) {
            case GL11_GL_CULL_FACE -> RenderSystem.disableCull();
            case GL11_GL_DEPTH_TEST -> RenderSystem.disableDepthTest();
            case GL11_GL_BLEND -> RenderSystem.disableBlend();
            case GL11_GL_SCISSOR_TEST -> GL11.glDisable(GL11.GL_SCISSOR_TEST);
            case GL11_GL_TEXTURE_2D -> RenderSystem.disableTexture();
            case GL11_GL_POLYGON_OFFSET_FILL -> RenderSystem.disablePolygonOffset();
            default -> VoxelConstants.getLogger().warn("OpenGL - Invalid state received");
        }
    }

    public static void glBlendFunc(int srcFactor, int dstFactor) { RenderSystem.blendFunc(srcFactor, dstFactor); }

    public static void glBlendFuncSeparate(int srcFactorRGB, int dstFactorRGB, int srcFactorAlpha, int dstFactorAlpha) { RenderSystem.blendFuncSeparate(srcFactorRGB, dstFactorRGB, srcFactorAlpha, dstFactorAlpha); }

    public static void glClear(int mask) { RenderSystem.clear(mask, VoxelConstants.isSystemMacOS()); }

    public static void glClearColor(float red, float green, float blue, float alpha) { RenderSystem.clearColor(red, green, blue, alpha); }

    public static void glClearDepth(double depth) { RenderSystem.clearDepth(depth); }

    public static void glColor3f(float red, float green, float blue) { glColor4f(red, green, blue, 1.0f); }

    public static void glColor4f(float red, float green, float blue, float alpha) { RenderSystem.setShaderColor(red, green, blue, alpha); }

    public static void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) { RenderSystem.colorMask(red, green, blue, alpha); }

    public static void glDeleteTexture(int texture) { RenderSystem.deleteTexture(texture); }

    public static void glDepthMask(boolean mask) { RenderSystem.depthMask(mask); }

    public static int glGenTextures() { return GlStateManager._genTexture(); }

    public static int glGetTexLevelParameteri(int target, int level, int pname) { return GlStateManager._getTexLevelParameter(target, level, pname); }

    public static void glPixelStorei(int pname, int param) { RenderSystem.pixelStore(pname, param); }

    public static void glPolygonOffset(float factor, float units) { RenderSystem.polygonOffset(factor, units); }

    public static void glTexImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, IntBuffer pixels) { GlStateManager._texImage2D(target, level, internalFormat, width, height, border, format, type, pixels); }

    public static void glTexParameterf(int target, int pname, float param) { GlStateManager._texParameter(target, pname, param); }

    public static void glTexParameteri(int target, int pname, int param) { RenderSystem.texParameter(target, pname, param); }

    public static void glViewport(int x, int y, int width, int height) { RenderSystem.viewport(x, y, width, height); }

    public static void glBindTexture(int target, int texture) {
        if (target == GL11_GL_TEXTURE_2D) {
            RenderSystem.bindTexture(texture);
            return;
        }

        GL11.glBindTexture(target, texture);
    }

    public static void glGenerateMipmap(int target) { GL30.glGenerateMipmap(target); }

    public static void glGetFloatv(int pname, FloatBuffer params) { GL11.glGetFloatv(pname, params); }

    public static int glGetInteger(int pname) { return GL11.glGetInteger(pname); }

    public static void glGetTexImage(int tex, int level, int format, int type, ByteBuffer pixels) { GL11.glGetTexImage(tex, level, format, type, pixels); }

    public static void glGetTexImage(int tex, int level, int format, int type, IntBuffer pixels) { GL11.glGetTexImage(tex, level, format, type, pixels); }

    public static void glPopAttrib() { GL11.glPopAttrib(); }

    public static void glPushAttrib(int mask) { GL11.glPushAttrib(mask); }

    public static void glScissor(int x, int y, int width, int height) { GL11.glScissor(x, y, width, height); }

    public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, ByteBuffer pixels) { GL11.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels); }

    public static void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, IntBuffer pixels) { GL11.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels); }
}