package com.mamiyaotaru.voxelmap.textures;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import org.apache.commons.io.IOUtils;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.system.MemoryUtil;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;

public final class TextureUtilLegacy {
    private static final int DATA_BUFFER_SIZE = 4194304;
    private static final IntBuffer DATA_BUFFER = MemoryUtil.memCallocInt(DATA_BUFFER_SIZE);

    private TextureUtilLegacy() {
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

    public static void uploadSubTexture(int[] textureData, int width, int height, int originX, int originY) {
        uploadTextureSub(textureData, width, height, originX, originY);
    }

    private static void copyToBufferPos(int[] imageData, int offset, int length) {
        DATA_BUFFER.clear();
        DATA_BUFFER.put(imageData, offset, length);
        DATA_BUFFER.position(0).limit(length);
    }

    private static void uploadTextureSub(int[] imageData, int width, int height, int originX, int originY) {
        // FIXME 1.21.5
        // RenderSystem.texParameter(GL30C.GL_TEXTURE_2D, GL30C.GL_TEXTURE_MIN_FILTER, GL30C.GL_NEAREST);
        // RenderSystem.texParameter(GL30C.GL_TEXTURE_2D, GL30C.GL_TEXTURE_MAG_FILTER, GL30C.GL_NEAREST);
        // RenderSystem.texParameter(GL30C.GL_TEXTURE_2D, GL30C.GL_TEXTURE_WRAP_S, GL30C.GL_REPEAT);
        // RenderSystem.texParameter(GL30C.GL_TEXTURE_2D, GL30C.GL_TEXTURE_WRAP_T, GL30C.GL_REPEAT);
        // RenderSystem.pixelStore(GL30C.GL_UNPACK_ROW_LENGTH, width);
        // RenderSystem.pixelStore(GL30C.GL_UNPACK_SKIP_PIXELS, 0);
        // RenderSystem.pixelStore(GL30C.GL_UNPACK_SKIP_ROWS, 0);

        int maxRows = DATA_BUFFER_SIZE / width;
        int rowsToCopy;
        for (int pos = 0; pos < width * height; pos += width * rowsToCopy) {
            int rowsCopied = pos / width;
            rowsToCopy = Math.min(maxRows, height - rowsCopied);
            int sizeOfCopy = width * rowsToCopy;
            copyToBufferPos(imageData, pos, sizeOfCopy);
            GL30C.glTexSubImage2D(GL30C.GL_TEXTURE_2D, 0, originX, originY + rowsCopied, width, rowsToCopy, GL30C.GL_BGRA, GL30C.GL_UNSIGNED_INT_8_8_8_8_REV, DATA_BUFFER);
        }
    }
}
