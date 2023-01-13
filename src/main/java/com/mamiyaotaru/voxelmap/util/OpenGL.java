package com.mamiyaotaru.voxelmap.util;

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
            GL12_GL_UNSIGNED_INT_8_8_8_8_REV = 0x8367,
            GL30_GL_READ_FRAMEBUFFER         = 0x8CA8,
            GL30_GL_DRAW_FRAMEBUFFER         = 0x8CA9;

    private OpenGL() {}
}