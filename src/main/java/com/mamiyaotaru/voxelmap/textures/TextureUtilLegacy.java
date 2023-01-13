package com.mamiyaotaru.voxelmap.textures;

import com.mamiyaotaru.voxelmap.util.OpenGL;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

public class TextureUtilLegacy {
    private static final IntBuffer DATA_BUFFER = createDirectIntBuffer(4194304);

    public static void deleteTexture(int textureId) {
        OpenGL.glDeleteTexture(textureId);
    }

    public static synchronized ByteBuffer createDirectByteBuffer(int capacity) {
        return ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
    }

    public static IntBuffer createDirectIntBuffer(int capacity) {
        return createDirectByteBuffer(capacity << 2).asIntBuffer();
    }

    public static BufferedImage readBufferedImage(InputStream imageStream) throws IOException {
        if (imageStream == null) {
            return null;
        } else {
            BufferedImage bufferedimage;
            try {
                bufferedimage = ImageIO.read(imageStream);
            } finally {
                IOUtils.closeQuietly(imageStream);
            }

            return bufferedimage;
        }
    }

    public static int uploadTextureImage(int textureId, BufferedImage texture) {
        return uploadTextureImageAllocate(textureId, texture, false, false);
    }

    public static int uploadTextureImageAllocate(int textureId, BufferedImage texture, boolean blur, boolean clamp) {
        allocateTexture(textureId, texture.getWidth(), texture.getHeight());
        return uploadTextureImageSub(textureId, texture, 0, 0, blur, clamp);
    }

    public static void allocateTexture(int textureId, int width, int height) {
        allocateTextureImpl(textureId, 0, width, height);
    }

    public static void allocateTextureImpl(int glTextureId, int mipmapLevels, int width, int height) {
        bindTexture(glTextureId);
        if (mipmapLevels >= 0) {
            OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, 33085, mipmapLevels);
            OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, 33082, 0);
            OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, 33083, mipmapLevels);
            OpenGL.glTexParameterf(OpenGL.GL11_GL_TEXTURE_2D, 34049, 0.0F);
        }

        for (int i = 0; i <= mipmapLevels; ++i) {
            OpenGL.glPixelStorei(OpenGL.GL11_GL_UNPACK_ROW_LENGTH, 0);
            OpenGL.glPixelStorei(OpenGL.GL11_GL_UNPACK_SKIP_PIXELS, 0);
            OpenGL.glPixelStorei(OpenGL.GL11_GL_UNPACK_SKIP_ROWS, 0);
            OpenGL.glTexImage2D(OpenGL.GL11_GL_TEXTURE_2D, i, OpenGL.GL11_GL_RGBA, width >> i, height >> i, 0, OpenGL.GL12_GL_BGRA, OpenGL.GL12_GL_UNSIGNED_INT_8_8_8_8_REV, (IntBuffer) null);
        }

    }

    public static int uploadTextureImageSub(int textureId, BufferedImage p_110995_1_, int p_110995_2_, int p_110995_3_, boolean p_110995_4_, boolean p_110995_5_) {
        bindTexture(textureId);
        uploadTextureImageSubImpl(p_110995_1_, p_110995_2_, p_110995_3_, p_110995_4_, p_110995_5_);
        return textureId;
    }

    private static void uploadTextureImageSubImpl(BufferedImage p_110993_0_, int p_110993_1_, int p_110993_2_, boolean p_110993_3_, boolean p_110993_4_) {
        int i = p_110993_0_.getWidth();
        int j = p_110993_0_.getHeight();
        int k = 4194304 / i;
        int[] aint = new int[k * i];
        setTextureBlurred(p_110993_3_);
        setTextureClamped(p_110993_4_);
        OpenGL.glPixelStorei(OpenGL.GL11_GL_UNPACK_ROW_LENGTH, 0);
        OpenGL.glPixelStorei(OpenGL.GL11_GL_UNPACK_SKIP_PIXELS, 0);
        OpenGL.glPixelStorei(OpenGL.GL11_GL_UNPACK_SKIP_ROWS, 0);

        for (int l = 0; l < i * j; l += i * k) {
            int i1 = l / i;
            int j1 = Math.min(k, j - i1);
            int k1 = i * j1;
            p_110993_0_.getRGB(0, i1, i, j1, aint, 0, i);
            copyToBuffer(aint, k1);
            OpenGL.glTexSubImage2D(OpenGL.GL11_GL_TEXTURE_2D, 0, p_110993_1_, p_110993_2_ + i1, i, j1, OpenGL.GL12_GL_BGRA, OpenGL.GL12_GL_UNSIGNED_INT_8_8_8_8_REV, DATA_BUFFER);
        }

    }

    public static void uploadTexture(int glTextureId, int[] zeros, int currentImageWidth, int currentImageHeight) {
        bindTexture(glTextureId);
        uploadTextureSub(0, zeros, currentImageWidth, currentImageHeight, 0, 0, false, false, false);
    }

    private static void copyToBuffer(int[] p_110990_0_, int p_110990_1_) {
        copyToBufferPos(p_110990_0_, 0, p_110990_1_);
    }

    private static void copyToBufferPos(int[] imageData, int p_110994_1_, int p_110994_2_) {
        DATA_BUFFER.clear();
        DATA_BUFFER.put(imageData, p_110994_1_, p_110994_2_);
        DATA_BUFFER.position(0).limit(p_110994_2_);
    }

    static void bindTexture(int id) {
        OpenGL.glBindTexture(OpenGL.GL11_GL_TEXTURE_2D, id);
    }

    public static void uploadTextureMipmap(int[][] textureData, int width, int height, int originX, int originY, boolean blurred, boolean clamped) {
        for (int i = 0; i < textureData.length; ++i) {
            int[] aint = textureData[i];
            uploadTextureSub(i, aint, width >> i, height >> i, originX >> i, originY >> i, blurred, clamped, textureData.length > 1);
        }

    }

    public static void setTextureClamped(boolean clamped) {
        if (clamped) {
            OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_WRAP_S, OpenGL.GL12_GL_CLAMP_TO_EDGE);
            OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_WRAP_T, OpenGL.GL12_GL_CLAMP_TO_EDGE);
        } else {
            OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_WRAP_S, 10497);
            OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_WRAP_T, 10497);
        }

    }

    private static void setTextureBlurred(boolean p_147951_0_) {
        setTextureBlurMipmap(p_147951_0_, false);
    }

    public static void setTextureBlurMipmap(boolean blurred, boolean mipmapped) {
        if (blurred) {
            OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MIN_FILTER, mipmapped ? OpenGL.GL11_GL_LINEAR_MIPMAP_LINEAR : OpenGL.GL11_GL_LINEAR);
            OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MAG_FILTER, OpenGL.GL11_GL_LINEAR);
        } else {
            OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MIN_FILTER, mipmapped ? 9986 : OpenGL.GL11_GL_NEAREST);
            OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MAG_FILTER, OpenGL.GL11_GL_NEAREST);
        }

    }

    private static void uploadTextureSub(int mipmapLevel, int[] imageData, int width, int height, int originX, int originY, boolean blurred, boolean clamped, boolean mipmapped) {
        int maxRows = 4194304 / width;
        setTextureBlurMipmap(blurred, mipmapped);
        setTextureClamped(clamped);
        OpenGL.glPixelStorei(OpenGL.GL11_GL_UNPACK_ROW_LENGTH, width);
        OpenGL.glPixelStorei(OpenGL.GL11_GL_UNPACK_SKIP_PIXELS, 0);
        OpenGL.glPixelStorei(OpenGL.GL11_GL_UNPACK_SKIP_ROWS, 0);

        int rowsToCopy;
        for (int pos = 0; pos < width * height; pos += width * rowsToCopy) {
            int rowsCopied = pos / width;
            rowsToCopy = Math.min(maxRows, height - rowsCopied);
            int sizeOfCopy = width * rowsToCopy;
            copyToBufferPos(imageData, pos, sizeOfCopy);
            OpenGL.glTexSubImage2D(OpenGL.GL11_GL_TEXTURE_2D, mipmapLevel, originX, originY + rowsCopied, width, rowsToCopy, OpenGL.GL12_GL_BGRA, OpenGL.GL12_GL_UNSIGNED_INT_8_8_8_8_REV, DATA_BUFFER);
        }

    }
}
