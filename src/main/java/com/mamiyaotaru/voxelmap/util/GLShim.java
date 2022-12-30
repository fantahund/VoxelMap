package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class GLShim {
    public static void glEnable(int attrib) {
        switch (attrib) {
            case GL11.GL_CULL_FACE -> RenderSystem.enableCull();
            case GL11.GL_DEPTH_TEST -> RenderSystem.enableDepthTest();
            case GL11.GL_BLEND -> RenderSystem.enableBlend();
            case GL11.GL_SCISSOR_TEST -> GL11.glEnable(GL11.GL_SCISSOR_TEST);
            case GL11.GL_TEXTURE_2D -> RenderSystem.enableTexture();
            case GL11.GL_POLYGON_OFFSET_FILL -> RenderSystem.enablePolygonOffset();
        }

    }

    public static void glDisable(int attrib) {
        switch (attrib) {
            case GL11.GL_CULL_FACE -> RenderSystem.disableCull();
            case GL11.GL_DEPTH_TEST -> RenderSystem.disableDepthTest();
            case GL11.GL_BLEND -> RenderSystem.disableBlend();
            case GL11.GL_SCISSOR_TEST -> GL11.glDisable(GL11.GL_SCISSOR_TEST);
            case GL11.GL_TEXTURE_2D -> RenderSystem.disableTexture();
            case GL11.GL_POLYGON_OFFSET_FILL -> RenderSystem.disablePolygonOffset();
        }

    }

    public static void glBlendFunc(int sfactor, int dfactor) {
        RenderSystem.blendFunc(sfactor, dfactor);
    }

    public static void glBlendFuncSeparate(int sfactorRGB, int dfactorRGB, int sfactorAlpha, int dfactorAlpha) {
        RenderSystem.blendFuncSeparate(sfactorRGB, dfactorRGB, sfactorAlpha, dfactorAlpha);
    }

    public static void glClear(int mask) {
        RenderSystem.clear(mask, VoxelConstants.isSystemMacOS());
    }

    public static void glClearColor(float red, float green, float blue, float alpha) {
        RenderSystem.clearColor(red, green, blue, alpha);
    }

    public static void glClearDepth(double depth) {
        RenderSystem.clearDepth(depth);
    }

    public static void glColor3f(float red, float green, float blue) {
        RenderSystem.setShaderColor(red, green, blue, 1.0F);
    }

    public static void glColor4f(float red, float green, float blue, float alpha) {
        RenderSystem.setShaderColor(red, green, blue, alpha);
    }

    public static void glColor3ub(int red, int green, int blue) {
        RenderSystem.setShaderColor((float) red / 255.0F, (float) green / 255.0F, (float) blue / 255.0F, 1.0F);
    }

    public static void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        RenderSystem.colorMask(red, green, blue, alpha);
    }

    public static void glDeleteTextures(int id) {
        RenderSystem.deleteTexture(id);
    }

    public static void glDepthFunc(int func) {
        RenderSystem.depthFunc(func);
    }

    public static void glDepthMask(boolean flag) {
        RenderSystem.depthMask(flag);
    }

    public static int glGenTextures() {
        return GlStateManager._genTexture();
    }

    public static void glGetTexImage(int tex, int level, int format, int type, long pixels) {
        GlStateManager._getTexImage(tex, level, format, type, pixels);
    }

    public static int glGetTexLevelParameteri(int target, int level, int pname) {
        return GlStateManager._getTexLevelParameter(target, level, pname);
    }

    public static void glLogicOp(int opcode) {
        GlStateManager._logicOp(opcode);
    }

    public static void glPixelStorei(int parameterName, int parameter) {
        RenderSystem.pixelStore(parameterName, parameter);
    }

    public static void glPolygonOffset(float factor, float units) {
        RenderSystem.polygonOffset(factor, units);
    }

    public static void glSetActiveTextureUnit(int texture) {
        RenderSystem.activeTexture(texture);
    }

    public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, IntBuffer pixels) {
        GlStateManager._texImage2D(target, level, internalformat, width, height, border, format, type, pixels);
    }

    public static void glTexParameterf(int target, int pname, float param) {
        GlStateManager._texParameter(target, pname, param);
    }

    public static void glTexParameteri(int target, int pname, int param) {
        RenderSystem.texParameter(target, pname, param);
    }

    public static void glTexSubImage2D(int target, int level, int xOffset, int yOffset, int width, int height, int format, int type, long memAddress) {
        GlStateManager._texSubImage2D(target, level, xOffset, yOffset, width, height, format, type, memAddress);
    }

    public static void glViewport(int x, int y, int width, int height) {
        RenderSystem.viewport(x, y, width, height);
    }

    public static void glBindTexture(int target, int texture) {
        if (target == GL11.GL_TEXTURE_2D) {
            RenderSystem.bindTexture(texture);
        } else {
            GL11.glBindTexture(target, texture);
        }

    }

    public static void glGenerateMipmap(int glTexture2d) {
        GL30.glGenerateMipmap(glTexture2d);
    }

    public static boolean glGetBoolean(int pname) {
        return GL11.glGetBoolean(pname);
    }

    public static void glGetFloatv(int pname, FloatBuffer params) {
        GL11.glGetFloatv(pname, params);
    }

    public static int glGetInteger(int pname) {
        return GL11.glGetInteger(pname);
    }

    public static void glGetTexImage(int tex, int level, int format, int type, ByteBuffer pixels) {
        GL11.glGetTexImage(tex, level, format, type, pixels);
    }

    public static void glGetTexImage(int tex, int level, int format, int type, IntBuffer pixels) {
        GL11.glGetTexImage(tex, level, format, type, pixels);
    }

    public static void glPopAttrib() {
        GL11.glPopAttrib();
    }

    public static void glPushAttrib(int mask) {
        GL11.glPushAttrib(mask);
    }

    public static void glScissor(int x, int y, int width, int height) {
        GL11.glScissor(x, y, width, height);
    }

    public static void glTexImage2D(int glTexture2d, int level, int glRgba, int width, int height, int border, int format, int type, ByteBuffer pixels) {
        GL11.glTexImage2D(glTexture2d, level, glRgba, width, height, border, format, type, pixels);
    }

    public static void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, IntBuffer pixels) {
        GL11.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels);
    }

    public static void glVertex2f(float x, float y) {
        GL11.glVertex2f(x, y);
    }

    public static void glVertex3f(float x, float y, float z) {
        GL11.glVertex3f(x, y, z);
    }
}
