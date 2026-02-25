package com.mamiyaotaru.voxelmap.util;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/**
 * from lightmap.fsh + net.minecraft.client.renderer.LightTexture
 */
public class CPULightmap {
    private static final CPULightmap INSTANCE = new CPULightmap();
    private static final Minecraft MINECRAFT = Minecraft.getInstance();

    private float blockLightRedFlicker = 0.0F;

    private float ambientLightFactor = 0.0F;
    private float skyFactor = 0.0F;
    private float blockFactor = 0.0F;
    private float skyLightColorR = 1.0F;
    private float skyLightColorG = 1.0F;
    private float skyLightColorB = 1.0F;
    private float brightnessFactor = 0.0F;

    public static CPULightmap getInstance() {
        return INSTANCE;
    }

    float mix(float v1, float v2, float mix) {
        return v1 * (1.0F - mix) + v2 * mix;
    }

    float getBrightness(float level) {
        return level / (4.0F - 3.0F * level);
    }

    float notGamma(float x) {
        float nx = 1.0F - x;
        return 1.0F - nx * nx * nx * nx;
    }

    public void setup() {
        ambientLightFactor = MINECRAFT.level.dimensionType().ambientLight();

        float g = 1.0F - (MINECRAFT.level.getSkyDarken() / 15.0F);
        float h = g * 0.95F + 0.05F;

        skyFactor = h;

        blockLightRedFlicker = blockLightRedFlicker + (float) ((Math.random() - Math.random()) * Math.random() * Math.random() * 0.1);
        blockLightRedFlicker *= 0.9F;
        blockFactor = blockLightRedFlicker + 1.5F;

        skyLightColorR = g * 0.65F + 0.35F;
        skyLightColorG = g * 0.65F + 0.35F;
        skyLightColorB = 1.0F;

        float p = MINECRAFT.options.gamma().get().floatValue();
        brightnessFactor = Math.max(0.0F, p);
    }

    public int getLight(int blockLight, int skyLight) {
        float blockBrightness = getBrightness(blockLight / 15.0F) * blockFactor;
        float skyBrightness = getBrightness(skyLight / 15.0F) * skyFactor;

        // cubic nonsense, dips to yellowish in the middle, white when fully saturated
        float colorR = blockBrightness;
        float colorG = blockBrightness * ((blockBrightness * 0.6F + 0.4F) * 0.6F + 0.4F);
        float colorB = blockBrightness * (blockBrightness * blockBrightness * 0.6F + 0.4F);

        colorR = mix(colorR, 1.0F, ambientLightFactor);
        colorG = mix(colorG, 1.0F, ambientLightFactor);
        colorB = mix(colorB, 1.0F, ambientLightFactor);

        colorR += skyLightColorR * skyBrightness;
        colorG += skyLightColorG * skyBrightness;
        colorB += skyLightColorB * skyBrightness;

        colorR = mix(colorR, 0.75F, 0.04F);
        colorG = mix(colorG, 0.75F, 0.04F);
        colorB = mix(colorB, 0.75F, 0.04F);

        colorR = Mth.clamp(colorR, 0.0F, 1.0F);
        colorG = Mth.clamp(colorG, 0.0F, 1.0F);
        colorB = Mth.clamp(colorB, 0.0F, 1.0F);

        colorR = mix(colorR, notGamma(colorR), brightnessFactor);
        colorG = mix(colorG, notGamma(colorG), brightnessFactor);
        colorB = mix(colorB, notGamma(colorB), brightnessFactor);

        colorR = mix(colorR, 0.75F, 0.04F);
        colorG = mix(colorG, 0.75F, 0.04F);
        colorB = mix(colorB, 0.75F, 0.04F);

        colorR = Math.min(colorR, 1.0F);
        colorG = Math.min(colorG, 1.0F);
        colorB = Math.min(colorB, 1.0F);

        return ARGB.colorFromFloat(1.0F, colorB, colorG, colorR);
    }
}
