package com.mamiyaotaru.voxelmap.util;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ARGB;
import org.joml.Vector3f;

/**
 * from lightmap.fsh + net.minecraft.client.renderer.LightTexture
 */
public class CPULightmap {
    private static final CPULightmap INSTANCE = new CPULightmap();
    private static final Minecraft MINECRAFT = Minecraft.getInstance();

    private float blockLightRedFlicker = 0.0f;

    private float AmbientLightFactor = 0.0f;
    private float SkyFactor = 0.0f;
    private float BlockFactor = 0.0f;
    private float SkyLightColorR = 1.0f;
    private float SkyLightColorG = 1.0f;
    private float SkyLightColorB = 1.0f;

    public static CPULightmap getInstance() {
        return INSTANCE;
    }

    float mix(float v1, float v2, float mix) {
        return v1 * (1 - mix) + v2 * mix;
    }

    float get_brightness(float level) {
        float curved_level = level / (4.0f - 3.0f * level);
        return mix(curved_level, 1.0f, AmbientLightFactor);
    }

    public void setup() {
        AmbientLightFactor = MINECRAFT.level.dimensionType().ambientLight();

        float g = MINECRAFT.level.getSkyDarken(1.0F);
        float h = g * 0.95F + 0.05F;

        SkyFactor = h;

        this.blockLightRedFlicker = this.blockLightRedFlicker + (float) ((Math.random() - Math.random()) * Math.random() * Math.random() * 0.1);
        this.blockLightRedFlicker *= 0.9F;
        BlockFactor = this.blockLightRedFlicker + 1.5F;

        SkyLightColorR = g * 0.65f + 0.35f;
        SkyLightColorG = g * 0.65f + 0.35f;
        SkyLightColorB = 1f;
    }

    public int getLight(int blockLight, int skyLight) {
        float block_brightness = get_brightness(blockLight / 15f) * BlockFactor;
        float sky_brightness = get_brightness(skyLight / 15f) * SkyFactor;

        // cubic nonsense, dips to yellowish in the middle, white when fully saturated
        float colorr = block_brightness;
        float colorg = block_brightness * ((block_brightness * 0.6f + 0.4f) * 0.6f + 0.4f);
        float colorb = block_brightness * (block_brightness * block_brightness * 0.6f + 0.4f);

        colorr += SkyLightColorR * sky_brightness;
        colorg += SkyLightColorG * sky_brightness;
        colorb += SkyLightColorB * sky_brightness;

        colorr = mix(colorr, 0.75f, 0.04f);
        colorg = mix(colorg, 0.75f, 0.04f);
        colorb = mix(colorb, 0.75f, 0.04f);

        colorr = Math.min(colorr, 1.0f);
        colorg = Math.min(colorg, 1.0f);
        colorb = Math.min(colorb, 1.0f);

        return ARGB.colorFromFloat(1.0f, colorb, colorg, colorr);
    }
}
