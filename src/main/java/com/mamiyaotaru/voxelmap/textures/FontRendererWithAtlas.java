package com.mamiyaotaru.voxelmap.textures;

import com.google.common.collect.Lists;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.util.GLShim;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.font.BlankFont;
import net.minecraft.client.font.Font;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.profiler.Profiler;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class FontRendererWithAtlas extends TextRenderer implements ResourceReloader {
    private final int[] charWidthArray = new int[256];
    public final int FONT_HEIGHT = 9;
    public final Random fontRandom = new Random();
    private final Identifier locationFontTexture;
    private Sprite fontIcon = null;
    private Sprite blankIcon = null;
    private float posX;
    private float posY;
    private float red;
    private float blue;
    private float green;
    private float alpha;
    private boolean randomStyle;
    private boolean boldStyle;
    private boolean italicStyle;
    private boolean underlineStyle;
    private boolean strikethroughStyle;
    private final BufferBuilder vertexBuffer;

    public FontRendererWithAtlas(TextureManager renderEngine, Identifier locationFontTexture) {
        super(identifierx -> Util.make(new FontStorage(renderEngine, locationFontTexture), fontStorage -> fontStorage.setFonts(Lists.newArrayList(new Font[]{new BlankFont()}))), true);
        this.locationFontTexture = locationFontTexture;
        renderEngine.bindTexture(this.locationFontTexture);

        for (int colorCodeIndex = 0; colorCodeIndex < 32; ++colorCodeIndex) {
            int var6 = (colorCodeIndex >> 3 & 1) * 85;
            int red = (colorCodeIndex >> 2 & 1) * 170 + var6;
            int green = (colorCodeIndex >> 1 & 1) * 170 + var6;
            int blue = (colorCodeIndex & 1) * 170 + var6;
            if (colorCodeIndex == 6) {
                red += 85;
            }

            if (colorCodeIndex >= 16) {
                red /= 4;
                green /= 4;
                blue /= 4;
            }

            int[] colorCode = new int[32];
            colorCode[colorCodeIndex] = (red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF;
        }

        this.vertexBuffer = Tessellator.getInstance().getBuffer();
    }

    public void onResourceManagerReload(ResourceManager resourceManager) {
        this.readFontTexture();
    }

    private void readFontTexture() {
        BufferedImage fontImage;
        try {
            fontImage = TextureUtilLegacy.readBufferedImage(VoxelConstants.getMinecraft().getResourceManager().getResource(this.locationFontTexture).get().getInputStream());
        } catch (IOException var17) {
            throw new RuntimeException(var17);
        }

        if (fontImage.getWidth() > 512 || fontImage.getHeight() > 512) {
            int maxDim = Math.max(fontImage.getWidth(), fontImage.getHeight());
            float scaleBy = 512.0F / (float) maxDim;
            int type = fontImage.getType();
            if (type == 13) {
                type = 6;
            }

            int newWidth = Math.max(1, (int) ((float) fontImage.getWidth() * scaleBy));
            int newHeight = Math.max(1, (int) ((float) fontImage.getHeight() * scaleBy));
            BufferedImage tmp = new BufferedImage(newWidth, newHeight, type);
            Graphics2D g2 = tmp.createGraphics();
            g2.drawImage(fontImage, 0, 0, newWidth, newHeight, null);
            g2.dispose();
            fontImage = tmp;
        }

        int sheetWidth = fontImage.getWidth();
        int sheetHeight = fontImage.getHeight();
        int[] sheetImageData = new int[sheetWidth * sheetHeight];
        fontImage.getRGB(0, 0, sheetWidth, sheetHeight, sheetImageData, 0, sheetWidth);
        int characterHeight = sheetHeight / 16;
        int characterWidth = sheetWidth / 16;
        byte padding = 1;
        float scale = 8.0F / (float) characterWidth;

        for (int characterIndex = 0; characterIndex < 256; ++characterIndex) {
            int characterX = characterIndex % 16;
            int characterY = characterIndex / 16;
            if (characterIndex == 32) {
                this.charWidthArray[characterIndex] = 3 + padding;
            }

            int thisCharacterWidth = characterWidth - 1;
            boolean onlyBlankPixels = true;

            while (thisCharacterWidth >= 0 && onlyBlankPixels) {
                int pixelX = characterX * characterWidth + thisCharacterWidth;

                for (int characterPixelYPos = 0; characterPixelYPos < characterHeight && onlyBlankPixels; ++characterPixelYPos) {
                    int pixelY = (characterY * characterWidth + characterPixelYPos) * sheetWidth;
                    if ((sheetImageData[pixelX + pixelY] >> 24 & 0xFF) != 0) {
                        onlyBlankPixels = false;
                        break;
                    }
                }

                if (onlyBlankPixels) {
                    --thisCharacterWidth;
                }
            }

            ++thisCharacterWidth;
            this.charWidthArray[characterIndex] = (int) (0.5 + (double) ((float) thisCharacterWidth * scale)) + padding;
        }

    }

    public void setSprites(Sprite text, Sprite blank) {
        this.fontIcon = text;
        this.blankIcon = blank;
    }

    private float renderCharAtPos(int charIndex, char character, boolean shadow) {
        return character == ' ' ? 4.0F : this.renderDefaultChar(charIndex, shadow);
    }

    private float renderDefaultChar(int charIndex, boolean shadow) {
        float sheetWidth = (float) (this.fontIcon.originX + this.fontIcon.width) / this.fontIcon.getMaxU();
        float sheetHeight = (float) (this.fontIcon.originY + this.fontIcon.height) / this.fontIcon.getMaxV();
        float fontScaleX = (float) (this.fontIcon.width - 2) / 128.0F;
        float fontScaleY = (float) (this.fontIcon.height - 2) / 128.0F;
        float charXPosInSheet = (float) (charIndex % 16 * 8) * fontScaleX + (float) this.fontIcon.originX + 1.0F;
        float charYPosInSheet = (float) (charIndex / 16 * 8) * fontScaleY + (float) this.fontIcon.originY + 1.0F;
        float shadowOffset = shadow ? 1.0F : 0.0F;
        float charWidth = (float) this.charWidthArray[charIndex] - 0.01F;
        this.vertexBuffer.vertex(this.posX + shadowOffset, this.posY, 0.0).texture(charXPosInSheet / sheetWidth, charYPosInSheet / sheetHeight).color(this.red, this.blue, this.green, this.alpha).next();
        this.vertexBuffer.vertex(this.posX - shadowOffset, this.posY + 7.99F, 0.0).texture(charXPosInSheet / sheetWidth, (charYPosInSheet + 7.99F * fontScaleY) / sheetHeight).color(this.red, this.blue, this.green, this.alpha).next();
        float var1 = (charXPosInSheet + (charWidth - 1.0F) * fontScaleX) / sheetWidth;
        this.vertexBuffer.vertex(this.posX + charWidth - 1.0F - shadowOffset, this.posY + 7.99F, 0.0).texture(var1, (charYPosInSheet + 7.99F * fontScaleY) / sheetHeight).color(this.red, this.blue, this.green, this.alpha).next();
        this.vertexBuffer.vertex(this.posX + charWidth - 1.0F + shadowOffset, this.posY, 0.0).texture(var1, charYPosInSheet / sheetHeight).color(this.red, this.blue, this.green, this.alpha).next();
        return (float) this.charWidthArray[charIndex];
    }

    public int drawStringWithShadow(String text, float x, float y, int color) {
        return this.drawString(text, x, y, color, true);
    }

    public int drawString(String text, int x, int y, int color) {
        return this.drawString(text, (float) x, (float) y, color, false);
    }

    public int drawString(String text, float x, float y, int color, boolean shadow) {
        this.resetStyles();
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        this.vertexBuffer.reset();
        this.vertexBuffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        int var6;
        if (shadow) {
            var6 = this.renderString(text, x + 1.0F, y + 1.0F, color, true);
            var6 = Math.max(var6, this.renderString(text, x, y, color, false));
        } else {
            var6 = this.renderString(text, x, y, color, false);
        }

        BufferRenderer.drawWithGlobalProgram(this.vertexBuffer.end());
        return var6;
    }

    private void resetStyles() {
        this.randomStyle = false;
        this.boldStyle = false;
        this.italicStyle = false;
        this.underlineStyle = false;
        this.strikethroughStyle = false;
    }

    private void renderStringAtPos(String text, boolean shadow) {
        for (int textIndex = 0; textIndex < text.length(); ++textIndex) {
            char character = text.charAt(textIndex);
            if (character == 167 && textIndex + 1 < text.length()) {
                int formatCode = "0123456789abcdefklmnor".indexOf(text.toLowerCase().charAt(textIndex + 1));
                if (formatCode < 16) {
                    this.randomStyle = false;
                    this.boldStyle = false;
                    this.strikethroughStyle = false;
                    this.underlineStyle = false;
                    this.italicStyle = false;
                } else if (formatCode == 16) {
                    this.randomStyle = true;
                } else if (formatCode == 17) {
                    this.boldStyle = true;
                } else if (formatCode == 18) {
                    this.strikethroughStyle = true;
                } else if (formatCode == 19) {
                    this.underlineStyle = true;
                } else if (formatCode == 20) {
                    this.italicStyle = true;
                } else {
                    this.randomStyle = false;
                    this.boldStyle = false;
                    this.strikethroughStyle = false;
                    this.underlineStyle = false;
                    this.italicStyle = false;
                    GLShim.glColor4f(this.red, this.blue, this.green, this.alpha);
                }

                ++textIndex;
            } else {
                int charIndex = "ÀÁÂÈÊËÍÓÔÕÚßãõğİıŒœŞşŴŵžȇ\u0000\u0000\u0000\u0000\u0000\u0000\u0000 !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u0000ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜø£Ø×ƒáíóúñÑªº¿®¬½¼¡«»░▒▓│┤╡╢╖╕╣║╗╝╜╛┐└┴┬├─┼╞╟╚╔╩╦╠═╬╧╨╤╥╙╘╒╓╫╪┘┌█▄▌▐▀αβΓπΣσμτΦΘΩδ∞∅∈∩≡±≥≤⌠⌡÷≈°∙·√ⁿ²■\u0000".indexOf(character);
                if (charIndex != -1) {
                    float sheetWidth = (float) (this.blankIcon.originX + this.blankIcon.width) / this.blankIcon.getMaxU();
                    float sheetHeight = (float) (this.blankIcon.originY + this.blankIcon.height) / this.blankIcon.getMaxV();
                    float u = (float) (this.blankIcon.originX + 4) / sheetWidth;
                    float v = (float) (this.blankIcon.originY + 4) / sheetHeight;
                    if (this.randomStyle) {
                        int randomCharIndex;
                        do {
                            randomCharIndex = this.fontRandom.nextInt(this.charWidthArray.length);
                        } while (this.charWidthArray[charIndex] != this.charWidthArray[randomCharIndex]);

                        charIndex = randomCharIndex;
                    }

                    float offset = 1.0F;
                    float widthOfRenderedChar = this.renderCharAtPos(charIndex, character, this.italicStyle);
                    if (this.boldStyle) {
                        this.posX += offset;
                        this.renderCharAtPos(charIndex, character, this.italicStyle);
                        this.posX -= offset;
                        ++widthOfRenderedChar;
                    }

                    if (this.strikethroughStyle) {
                        this.vertexBuffer.vertex(this.posX, this.posY + (float) (this.FONT_HEIGHT / 2), 0.0).texture(u, v).color(this.red, this.blue, this.green, this.alpha).next();
                        this.vertexBuffer.vertex(this.posX + widthOfRenderedChar, this.posY + (float) (this.FONT_HEIGHT / 2), 0.0).texture(u, v).color(this.red, this.blue, this.green, this.alpha).next();
                        this.vertexBuffer.vertex(this.posX + widthOfRenderedChar, this.posY + (float) (this.FONT_HEIGHT / 2) - 1.0F, 0.0).texture(u, v).color(this.red, this.blue, this.green, this.alpha).next();
                        this.vertexBuffer.vertex(this.posX, this.posY + (float) (this.FONT_HEIGHT / 2) - 1.0F, 0.0).texture(u, v).color(this.red, this.blue, this.green, this.alpha).next();
                    }

                    if (this.underlineStyle) {
                        int l = -1;
                        this.vertexBuffer.vertex(this.posX + (float) l, this.posY + (float) this.FONT_HEIGHT, 0.0).texture(u, v).color(this.red, this.blue, this.green, this.alpha).next();
                        this.vertexBuffer.vertex(this.posX + widthOfRenderedChar, this.posY + (float) this.FONT_HEIGHT, 0.0).texture(u, v).color(this.red, this.blue, this.green, this.alpha).next();
                        this.vertexBuffer.vertex(this.posX + widthOfRenderedChar, this.posY + (float) this.FONT_HEIGHT - 1.0F, 0.0).texture(u, v).color(this.red, this.blue, this.green, this.alpha).next();
                        this.vertexBuffer.vertex(this.posX + (float) l, this.posY + (float) this.FONT_HEIGHT - 1.0F, 0.0).texture(u, v).color(this.red, this.blue, this.green, this.alpha).next();
                    }

                    this.posX += (float) ((int) widthOfRenderedChar);
                }
            }
        }

    }

    private int renderString(String text, float x, float y, int color, boolean shadow) {
        if (text == null) {
            return 0;
        } else {
            if ((color & -67108864) == 0) {
                color |= -16777216;
            }

            if (shadow) {
                color = (color & 16579836) >> 2 | color & 0xFF000000;
            }

            this.red = (float) (color >> 16 & 0xFF) / 255.0F;
            this.blue = (float) (color >> 8 & 0xFF) / 255.0F;
            this.green = (float) (color & 0xFF) / 255.0F;
            this.alpha = (float) (color >> 24 & 0xFF) / 255.0F;
            this.posX = x;
            this.posY = y;
            this.renderStringAtPos(text, shadow);
            return (int) this.posX;
        }
    }

    public int getStringWidth(String string) {
        if (string == null) {
            return 0;
        } else {
            int totalWidth = 0;
            boolean includeSpace = false;

            for (int charIndex = 0; charIndex < string.length(); ++charIndex) {
                char character = string.charAt(charIndex);
                float characterWidth = this.getCharWidth(character);
                if (characterWidth < 0.0F && charIndex < string.length() - 1) {
                    ++charIndex;
                    character = string.charAt(charIndex);
                    if (character != 'l' && character != 'L') {
                        if (character == 'r' || character == 'R') {
                            includeSpace = false;
                        }
                    } else {
                        includeSpace = true;
                    }

                    characterWidth = 0.0F;
                }

                totalWidth = (int) ((float) totalWidth + characterWidth);
                if (includeSpace && characterWidth > 0.0F) {
                    ++totalWidth;
                }
            }

            return totalWidth;
        }
    }

    public float getCharWidth(char character) {
        if (character == 167) {
            return -1.0F;
        } else if (character == ' ') {
            return 4.0F;
        } else {
            int indexInDefaultSheet = "ÀÁÂÈÊËÍÓÔÕÚßãõğİıŒœŞşŴŵžȇ\u0000\u0000\u0000\u0000\u0000\u0000\u0000 !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u0000ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜø£Ø×ƒáíóúñÑªº¿®¬½¼¡«»░▒▓│┤╡╢╖╕╣║╗╝╜╛┐└┴┬├─┼╞╟╚╔╩╦╠═╬╧╨╤╥╙╘╒╓╫╪┘┌█▄▌▐▀αβΓπΣσμτΦΘΩδ∞∅∈∩≡±≥≤⌠⌡÷≈°∙·√ⁿ²■\u0000".indexOf(character);
            return character > 0 && indexInDefaultSheet != -1 ? (float) this.charWidthArray[indexInDefaultSheet] : 0.0F;
        }
    }

    public CompletableFuture<Void> reload(ResourceReloader.Synchronizer var1, ResourceManager var2, Profiler var3, Profiler var4, Executor var5, Executor var6) {
        return null;
    }
}
