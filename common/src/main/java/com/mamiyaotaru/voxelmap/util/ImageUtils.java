package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.NativeImage.Format;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.textures.GpuTexture;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.ReloadableTexture;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryUtil;

public class ImageUtils {
    public static void saveImage(String name, GpuTexture texture) {
        saveImage(name, texture, 0, texture.getWidth(0), texture.getHeight(0));
    }
    public static void saveImage(String name, GpuTexture texture, int maxMipmapLevel, int width, int height) {
        TextureUtil.writeAsPNG(Paths.get(""), name, texture, maxMipmapLevel, i -> i);
    }

    public static BufferedImage validateImage(BufferedImage image) {
        if (image.getType() != 6) {
            BufferedImage temp = new BufferedImage(image.getWidth(), image.getHeight(), 6);
            Graphics2D g2 = temp.createGraphics();
            g2.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
            g2.dispose();
            image = temp;
        }

        return image;
    }

    public static NativeImage createNativeImageFromIdentifier(Identifier Identifier) {
        try {
            return TextureContents.load(Minecraft.getInstance().getResourceManager(), Identifier).image();
        } catch (Exception var5) {
            return null;
        }
    }

    public static BufferedImage createBufferedImageFromIdentifier(Identifier Identifier) {
        try {
            AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(Identifier);
            BufferedImage image = null;
            if (texture instanceof DynamicTexture dynamicTexture) {
                image = bufferedImageFromNativeImage(dynamicTexture.getPixels());
            } else if (texture instanceof ReloadableTexture) {
                InputStream is = VoxelConstants.getMinecraft().getResourceManager().getResource(Identifier).get().open();
                image = ImageIO.read(is);
                is.close();
                if (image.getType() != BufferedImage.TYPE_4BYTE_ABGR) {
                    BufferedImage temp = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
                    Graphics2D g2 = temp.createGraphics();
                    g2.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
                    g2.dispose();
                    image = temp;
                }
            }
            return image;
        } catch (Exception var5) {
            return null;
        }
    }

    public static NativeImage nativeImageFromBufferedImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        NativeImage nativeImage = new NativeImage(width, height, false);
        if (image.getType() == BufferedImage.TYPE_4BYTE_ABGR) {
            byte[] is = new byte[width * height * 4];
            image.getRaster().getDataElements(0, 0, width, height, is);
            MemoryUtil.memByteBuffer(nativeImage.getPointer(), is.length).put(is);
            return nativeImage;
        }
        VoxelConstants.getLogger().warn("ImageUtils.nativeImageFromBufferedImage: Unoptimized image format: " + image.getType(), new Exception());
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int argb = image.getRGB(x, y);
                nativeImage.setPixel(x, y, argb);
            }
        }
        return nativeImage;
    }

    public static BufferedImage bufferedImageFromNativeImage(NativeImage image) {
        if (image.getPointer() == 0) {
            throw new IllegalStateException("image is not allocated!");
        }
        if (image.format() != Format.RGBA) {
            throw new IllegalStateException("invalid format. expected RGBA, got " + image.format() + "!");
        }
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);

        // if (image.format() == Format.RGB) {
        // // unoptimized
        // for (int x = 0; x < width; x++) {
        // for (int y = 0; y < height; y++) {
        // int argb = image.getPixel(x, y);
        // bufferedImage.setRGB(x, y, argb);
        // }
        // }
        // return bufferedImage;
        // }

        byte[] is = new byte[width * height * image.format().components()];
        MemoryUtil.memByteBuffer(image.getPointer(), is.length).get(is);
        bufferedImage.getRaster().setDataElements(0, 0, width, height, is);
        return bufferedImage;
    }

    public static BufferedImage blankImage(Identifier Identifier, int w, int h) {
        return blankImage(Identifier, w, h, 64, 32);
    }

    public static BufferedImage blankImage(Identifier Identifier, int w, int h, int imageWidth, int imageHeight) {
        return blankImage(Identifier, w, h, imageWidth, imageHeight, 0, 0, 0, 0);
    }

    public static BufferedImage blankImage(Identifier Identifier, int w, int h, int r, int g, int b, int a) {
        return blankImage(Identifier, w, h, 64, 32, r, g, b, a);
    }

    public static BufferedImage blankImage(Identifier Identifier, int w, int h, int imageWidth, int imageHeight, int r, int g, int b, int a) {
        try {
            InputStream is = VoxelConstants.getMinecraft().getResourceManager().getResource(Identifier).get().open();
            BufferedImage mobSkin = ImageIO.read(is);
            is.close();
            BufferedImage temp = new BufferedImage(w * mobSkin.getWidth() / imageWidth, h * mobSkin.getWidth() / imageWidth, 6);
            Graphics2D g2 = temp.createGraphics();
            g2.setColor(new Color(r, g, b, a));
            g2.fillRect(0, 0, temp.getWidth(), temp.getHeight());
            g2.dispose();
            return temp;
        } catch (Exception var13) {
            VoxelConstants.getLogger().error("Failed getting mob: " + Identifier.toString() + " - " + var13.getLocalizedMessage(), var13);
            return null;
        }
    }

    public static BufferedImage blankImage(BufferedImage mobSkin, int w, int h) {
        return blankImage(mobSkin, w, h, 64, 32);
    }

    public static BufferedImage blankImage(BufferedImage mobSkin, int w, int h, int imageWidth, int imageHeight) {
        return blankImage(mobSkin, w, h, imageWidth, imageHeight, 0, 0, 0, 0);
    }

    public static BufferedImage blankImage(BufferedImage mobSkin, int w, int h, int r, int g, int b, int a) {
        return blankImage(mobSkin, w, h, 64, 32, r, g, b, a);
    }

    public static BufferedImage blankImage(BufferedImage mobSkin, int w, int h, int imageWidth, int imageHeight, int r, int g, int b, int a) {
        BufferedImage temp = new BufferedImage(w * mobSkin.getWidth() / imageWidth, h * mobSkin.getWidth() / imageWidth, 6);
        Graphics2D g2 = temp.createGraphics();
        g2.setColor(new Color(r, g, b, a));
        g2.fillRect(0, 0, temp.getWidth(), temp.getHeight());
        g2.dispose();
        return temp;
    }

    public static BufferedImage addCharacter(BufferedImage image, String character) {
        Graphics2D g2 = image.createGraphics();
        g2.setColor(new Color(0, 0, 0, 255));
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setFont(new Font("Arial", Font.PLAIN, image.getHeight()));
        FontMetrics fm = g2.getFontMetrics();
        int x = (image.getWidth() - fm.stringWidth("?")) / 2;
        int y = fm.getAscent() + (image.getHeight() - (fm.getAscent() + fm.getDescent())) / 2;
        g2.drawString("?", x, y);
        g2.dispose();
        return image;
    }

    public static BufferedImage eraseArea(BufferedImage image, int x, int y, int w, int h, int imageWidth, int imageHeight) {
        float scaleX = ((float) image.getWidth(null) / imageWidth);
        float scaleY = ((float) image.getHeight(null) / imageHeight);
        x = (int) (x * scaleX);
        y = (int) (y * scaleY);
        w = (int) (w * scaleX);
        h = (int) (h * scaleY);
        int[] blankPixels = new int[w * h];
        Arrays.fill(blankPixels, 0);
        image.setRGB(x, y, w, h, blankPixels, 0, w);
        return image;
    }

    public static BufferedImage loadImage(Identifier Identifier, int x, int y, int w, int h) {
        return loadImage(Identifier, x, y, w, h, 64, 32);
    }

    public static BufferedImage loadImage(Identifier Identifier, int x, int y, int w, int h, int imageWidth, int imageHeight) {
        BufferedImage mobSkin = createBufferedImageFromIdentifier(Identifier);
        if (mobSkin != null) {
            return loadImage(mobSkin, x, y, w, h, imageWidth, imageHeight);
        } else {
            VoxelConstants.getLogger().warn("Failed getting image: " + Identifier.toString());
            return null;
        }
    }

    public static BufferedImage loadImage(BufferedImage mobSkin, int x, int y, int w, int h) {
        return loadImage(mobSkin, x, y, w, h, 64, 32);
    }

    public static BufferedImage loadImage(BufferedImage mobSkin, int x, int y, int w, int h, int imageWidth, int imageHeight) {
        float scale = ((float) mobSkin.getWidth() / imageWidth);
        x = (int) (x * scale);
        y = (int) (y * scale);
        w = (int) (w * scale);
        h = (int) (h * scale);
        w = Math.max(1, w);
        h = Math.max(1, h);
        x = Math.min(mobSkin.getWidth() - w, x);
        y = Math.min(mobSkin.getHeight() - h, y);
        return mobSkin.getSubimage(x, y, w, h);
    }

    public static BufferedImage addImages(BufferedImage base, BufferedImage overlay, float x, float y, int baseWidth, int baseHeight) {
        int scale = base.getWidth() / baseWidth;
        Graphics gfx = base.getGraphics();
        gfx.drawImage(overlay, (int) (x * scale), (int) (y * scale), null);
        gfx.dispose();
        return base;
    }

    public static BufferedImage scaleImage(BufferedImage image, float scaleBy) {
        if (scaleBy == 1.0F) {
            return image;
        } else {
            int type = image.getType();
            if (type == BufferedImage.TYPE_BYTE_INDEXED) {
                type = BufferedImage.TYPE_4BYTE_ABGR;
            }

            int newWidth = Math.max(1, (int) (image.getWidth() * scaleBy));
            int newHeight = Math.max(1, (int) (image.getHeight() * scaleBy));
            BufferedImage tmp = new BufferedImage(newWidth, newHeight, type);
            Graphics2D g2 = tmp.createGraphics();
            g2.drawImage(image, 0, 0, newWidth, newHeight, null);
            g2.dispose();
            return tmp;
        }
    }

    public static BufferedImage scaleImage(BufferedImage image, float xScaleBy, float yScaleBy) {
        if (xScaleBy == 1.0F && yScaleBy == 1.0F) {
            return image;
        } else {
            int type = image.getType();
            if (type == BufferedImage.TYPE_BYTE_INDEXED) {
                type = BufferedImage.TYPE_4BYTE_ABGR;
            }

            int newWidth = Math.max(1, (int) (image.getWidth() * xScaleBy));
            int newHeight = Math.max(1, (int) (image.getHeight() * yScaleBy));
            BufferedImage tmp = new BufferedImage(newWidth, newHeight, type);
            Graphics2D g2 = tmp.createGraphics();
            g2.drawImage(image, 0, 0, newWidth, newHeight, null);
            g2.dispose();
            return tmp;
        }
    }

    public static BufferedImage flipHorizontal(BufferedImage image) {
        AffineTransform tx = AffineTransform.getScaleInstance(-1.0, 1.0);
        tx.translate(-image.getWidth(null), 0.0);
        AffineTransformOp op = new AffineTransformOp(tx, 1);
        return op.filter(image, null);
    }

    public static BufferedImage into128(BufferedImage base) {
        BufferedImage frame = new BufferedImage(128, 128, base.getType());
        Graphics gfx = frame.getGraphics();
        gfx.drawImage(base, 64 - base.getWidth() / 2, 64 - base.getHeight() / 2, base.getWidth(), base.getHeight(), null);
        gfx.dispose();
        return frame;
    }

    public static BufferedImage intoSquare(BufferedImage base) {
        int dim = Math.max(base.getWidth(), base.getHeight());
        int t = 1;

        while (Math.pow(2.0, t - 1) < dim) {
            ++t;
        }

        int size = (int) Math.pow(2.0, t);
        BufferedImage frame = new BufferedImage(size, size, base.getType());
        Graphics gfx = frame.getGraphics();
        gfx.drawImage(base, (size - base.getWidth()) / 2, (size - base.getHeight()) / 2, base.getWidth(), base.getHeight(), null);
        gfx.dispose();
        return frame;
    }

    public static BufferedImage pad(BufferedImage base) {
        int dim = Math.max(base.getWidth(), base.getHeight());
        int outlineWidth = 3;
        int size = dim + outlineWidth * 2;
        BufferedImage frame = new BufferedImage(size, size, base.getType());
        Graphics gfx = frame.getGraphics();
        gfx.drawImage(base, (size - base.getWidth()) / 2, (size - base.getHeight()) / 2, base.getWidth(), base.getHeight(), null);
        gfx.dispose();
        return frame;
    }

    public static BufferedImage fillOutline(BufferedImage image, boolean outline, int passes) {
        return fillOutline(image, outline, false, 0.0F, 0.0F, passes);
    }

    public static BufferedImage fillOutline(BufferedImage image, boolean outline, boolean armor, float intendedWidth, float intendedHeight, int passes) {
        if (outline) {
            for (int t = 0; t < passes; ++t) {
                image = fillOutline(image, true, armor, intendedWidth, intendedHeight);
            }
        }

        return fillOutline(image, false, armor, intendedWidth, intendedHeight);
    }

    private static BufferedImage fillOutline(BufferedImage image, boolean solid, boolean armor, float intendedWidth, float intendedHeight) {
        float armorOutlineFractionHorizontal = intendedWidth / 2.0F - 1.0F;
        float armorOutlineFractionVertical = intendedHeight / 2.0F - 1.0F;
        BufferedImage temp = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        Graphics gfx = temp.getGraphics();
        gfx.drawImage(image, 0, 0, null);
        gfx.dispose();
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        for (int t = 0; t < image.getWidth(); ++t) {
            for (int s = 0; s < image.getHeight(); ++s) {
                int color = image.getRGB(t, s);
                if ((color >> 24 & 0xFF) == 0) {
                    int newColor = sampleNonTransparentNeighborPixel(t, s, image);
                    if (newColor != -420) {
                        if (solid) {
                            if (armor && !(t <= (imageWidth / 2f) - armorOutlineFractionHorizontal) && !(t >= (imageWidth / 2f) + armorOutlineFractionHorizontal - 1.0F) && !(s <= (imageHeight / 2f) - armorOutlineFractionVertical) && !(s >= (imageHeight / 2f) + armorOutlineFractionVertical - 1.0F)) {
                                newColor = 0;
                            } else {
                                newColor = -16777216;
                            }
                        } else {
                            int red = newColor >> 16 & 0xFF;
                            int green = newColor >> 8 & 0xFF;
                            int blue = newColor & 0xFF;
                            newColor = (red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF;
                        }

                        temp.setRGB(t, s, newColor);
                    }
                }
            }
        }

        return temp;
    }

    private static int sampleNonTransparentNeighborPixel(int x, int y, BufferedImage image) {
        if (x > 0) {
            int color = image.getRGB(x - 1, y);
            if ((color >> 24 & 0xFF) > 50) {
                return color;
            }
        }

        if (x < image.getWidth() - 1) {
            int color = image.getRGB(x + 1, y);
            if ((color >> 24 & 0xFF) > 50) {
                return color;
            }
        }

        if (y > 0) {
            int color = image.getRGB(x, y - 1);
            if ((color >> 24 & 0xFF) > 50) {
                return color;
            }
        }

        if (y < image.getHeight() - 1) {
            int color = image.getRGB(x, y + 1);
            if ((color >> 24 & 0xFF) > 50) {
                return color;
            }
        }

        if (x > 0 && y > 0) {
            int color = image.getRGB(x - 1, y - 1);
            if ((color >> 24 & 0xFF) > 50) {
                return color;
            }
        }

        if (x > 0 && y < image.getHeight() - 1) {
            int color = image.getRGB(x - 1, y + 1);
            if ((color >> 24 & 0xFF) > 50) {
                return color;
            }
        }

        if (x < image.getWidth() - 1 && y > 0) {
            int color = image.getRGB(x + 1, y - 1);
            if ((color >> 24 & 0xFF) > 50) {
                return color;
            }
        }

        if (x < image.getWidth() - 1 && y < image.getHeight() - 1) {
            int color = image.getRGB(x + 1, y + 1);
            if ((color >> 24 & 0xFF) > 50) {
                return color;
            }
        }

        return -420;
    }

    public static NativeImage fillOutline(NativeImage image, boolean outline, int passes) {
        return fillOutline(image, outline, false, 0.0F, 0.0F, passes);
    }

    public static NativeImage fillOutline(NativeImage image, boolean outline, boolean armor, float intendedWidth, float intendedHeight, int passes) {
        if (outline) {
            for (int t = 0; t < passes; ++t) {
                image = fillOutline(image, true, armor, intendedWidth, intendedHeight);
            }
        }

        return fillOutline(image, false, armor, intendedWidth, intendedHeight);
    }

    private static NativeImage fillOutline(NativeImage image, boolean solid, boolean armor, float intendedWidth, float intendedHeight) {
        float armorOutlineFractionHorizontal = intendedWidth / 2.0F - 1.0F;
        float armorOutlineFractionVertical = intendedHeight / 2.0F - 1.0F;
        NativeImage temp = new NativeImage(image.getWidth(), image.getHeight(), false);
        temp.copyFrom(image);
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        for (int t = 0; t < image.getWidth(); ++t) {
            for (int s = 0; s < image.getHeight(); ++s) {
                int color = temp.getPixel(t, s);
                if ((color >> 24 & 0xFF) == 0) {
                    int newColor = sampleNonTransparentNeighborPixel(t, s, temp);
                    if (newColor != -420) {
                        if (solid) {
                            if (armor && !(t <= (imageWidth / 2f) - armorOutlineFractionHorizontal) && !(t >= (imageWidth / 2f) + armorOutlineFractionHorizontal - 1.0F) && !(s <= (imageHeight / 2f) - armorOutlineFractionVertical) && !(s >= (imageHeight / 2f) + armorOutlineFractionVertical - 1.0F)) {
                                newColor = 0;
                            } else {
                                newColor = -16777216;
                            }
                        } else {
                            int red = newColor >> 16 & 0xFF;
                            int green = newColor >> 8 & 0xFF;
                            int blue = newColor & 0xFF;
                            newColor = (red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF;
                        }

                        image.setPixel(t, s, newColor);
                    }
                }
            }
        }
        temp.close();
        return image;
    }

    private static int sampleNonTransparentNeighborPixel(int x, int y, NativeImage image) {
        if (x > 0) {
            int color = image.getPixel(x - 1, y);
            if ((color >> 24 & 0xFF) > 50) {
                return color;
            }
        }

        if (x < image.getWidth() - 1) {
            int color = image.getPixel(x + 1, y);
            if ((color >> 24 & 0xFF) > 50) {
                return color;
            }
        }

        if (y > 0) {
            int color = image.getPixel(x, y - 1);
            if ((color >> 24 & 0xFF) > 50) {
                return color;
            }
        }

        if (y < image.getHeight() - 1) {
            int color = image.getPixel(x, y + 1);
            if ((color >> 24 & 0xFF) > 50) {
                return color;
            }
        }

        if (x > 0 && y > 0) {
            int color = image.getPixel(x - 1, y - 1);
            if ((color >> 24 & 0xFF) > 50) {
                return color;
            }
        }

        if (x > 0 && y < image.getHeight() - 1) {
            int color = image.getPixel(x - 1, y + 1);
            if ((color >> 24 & 0xFF) > 50) {
                return color;
            }
        }

        if (x < image.getWidth() - 1 && y > 0) {
            int color = image.getPixel(x + 1, y - 1);
            if ((color >> 24 & 0xFF) > 50) {
                return color;
            }
        }

        if (x < image.getWidth() - 1 && y < image.getHeight() - 1) {
            int color = image.getPixel(x + 1, y + 1);
            if ((color >> 24 & 0xFF) > 50) {
                return color;
            }
        }

        return -420;
    }

    public static BufferedImage trim(BufferedImage image) {
        int left = -1;
        int right = image.getWidth();
        int top = -1;
        int bottom = image.getHeight();
        boolean foundColor = false;
        int color;

        while (!foundColor && left < right - 1) {
            ++left;

            for (int t = 0; t < image.getHeight(); ++t) {
                color = image.getRGB(left, t);
                if (color >> 24 != 0) {
                    foundColor = true;
                }
            }
        }

        foundColor = false;

        while (!foundColor && right > left) {
            --right;

            for (int t = 0; t < image.getHeight(); ++t) {
                color = image.getRGB(right, t);
                if (color >> 24 != 0) {
                    foundColor = true;
                }
            }
        }

        foundColor = false;

        while (!foundColor && top < bottom - 1) {
            ++top;

            for (int t = 0; t < image.getWidth(); ++t) {
                color = image.getRGB(t, top);
                if (color >> 24 != 0) {
                    foundColor = true;
                }
            }
        }

        foundColor = false;

        while (!foundColor && bottom > top) {
            --bottom;

            for (int t = 0; t < image.getWidth(); ++t) {
                color = image.getRGB(t, bottom);
                if (color >> 24 != 0) {
                    foundColor = true;
                }
            }
        }

        return image.getSubimage(left, top, right - left + 1, bottom - top + 1);
    }

    public static BufferedImage trimCentered(BufferedImage image) {
        int height = image.getHeight();
        int width = image.getWidth();
        int left = -1;
        int right = width;
        int top = -1;
        int bottom = height;
        boolean foundColor = false;
        int color;

        while (!foundColor && left < width / 2 - 1 && top < height / 2 - 1) {
            ++left;
            --right;
            ++top;
            --bottom;

            for (int y = top; y < bottom; ++y) {
                color = image.getRGB(left, y);
                if (color >> 24 != 0) {
                    foundColor = true;
                }
            }

            for (int y = top; y < bottom; ++y) {
                color = image.getRGB(right, y);
                if (color >> 24 != 0) {
                    foundColor = true;
                }
            }

            for (int x = left; x < right; ++x) {
                color = image.getRGB(x, top);
                if (color >> 24 != 0) {
                    foundColor = true;
                }
            }

            for (int x = left; x < right; ++x) {
                color = image.getRGB(x, bottom);
                if (color >> 24 != 0) {
                    foundColor = true;
                }
            }
        }

        return image.getSubimage(left, top, right - left + 1, bottom - top + 1);
    }

    public static BufferedImage colorify(BufferedImage image, float r, float g, float b) {
        BufferedImage temp = new BufferedImage(image.getWidth(), image.getHeight(), 3);
        Graphics2D gfx = temp.createGraphics();
        gfx.drawImage(image, 0, 0, null);
        gfx.dispose();

        for (int x = 0; x < temp.getWidth(); ++x) {
            for (int y = 0; y < temp.getHeight(); ++y) {
                int ax = temp.getColorModel().getAlpha(temp.getRaster().getDataElements(x, y, null));
                int rx = temp.getColorModel().getRed(temp.getRaster().getDataElements(x, y, null));
                int gx = temp.getColorModel().getGreen(temp.getRaster().getDataElements(x, y, null));
                int bx = temp.getColorModel().getBlue(temp.getRaster().getDataElements(x, y, null));
                rx = (int) (rx * r);
                gx = (int) (gx * g);
                bx = (int) (bx * b);
                temp.setRGB(x, y, ax << 24 | rx << 16 | gx << 8 | bx);
            }
        }

        return temp;
    }

    public static BufferedImage colorify(BufferedImage image, int r, int g, int b) {
        return colorify(image, r / 255.0F, g / 255.0F, b / 255.0F);
    }

    public static BufferedImage colorify(BufferedImage image, int rgb) {
        return colorify(image, rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF);
    }

    public static float percentageOfEdgePixelsThatAreSolid(BufferedImage image) {
        float edgePixels = (image.getWidth() * 2 + image.getHeight() * 2 - 2);
        float edgePixelsWithColor = 0.0F;
        int color;

        for (int t = 0; t < image.getHeight(); ++t) {
            color = image.getRGB(0, t);
            if (color >> 24 != 0) {
                ++edgePixelsWithColor;
            }

            color = image.getRGB(image.getWidth() - 1, t);
            if (color >> 24 != 0) {
                ++edgePixelsWithColor;
            }
        }

        for (int t = 1; t < image.getWidth() - 1; ++t) {
            color = image.getRGB(t, 0);
            if (color >> 24 != 0) {
                ++edgePixelsWithColor;
            }

            color = image.getRGB(t, image.getHeight() - 1);
            if (color >> 24 != 0) {
                ++edgePixelsWithColor;
            }
        }

        return edgePixelsWithColor / edgePixels;
    }
}
