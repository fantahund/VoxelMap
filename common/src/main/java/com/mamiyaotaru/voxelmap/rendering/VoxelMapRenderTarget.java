package com.mamiyaotaru.voxelmap.rendering;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.textures.AllocatedTexture;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;

public class VoxelMapRenderTarget extends RenderTarget {
    private static final GpuSampler DEFAULT_SAMPLER = RenderSystem.getSamplerCache().getSampler(AddressMode.REPEAT, AddressMode.REPEAT, FilterMode.LINEAR, FilterMode.LINEAR, false);
    public final Identifier textureId;
    private AllocatedTexture texture;

    public VoxelMapRenderTarget(String name, GpuFormat format, boolean useDepth) {
        super(name, useDepth, format);
        textureId = Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "render_target/" + UUID.randomUUID());
    }

    public Identifier getTextureLocation() {
        return textureId;
    }

    public AbstractTexture getTexture() {
        return texture;
    }

    @Override
    public void createBuffers(int width, int height) {
        super.createBuffers(width, height);
        texture = new AllocatedTexture(colorTexture, colorTextureView);
        texture.sampler = DEFAULT_SAMPLER;
        Minecraft.getInstance().getTextureManager().register(textureId, texture);
    }

    @Override
    public void destroyBuffers() {
        super.destroyBuffers();
        if (texture != null) {
            texture = null;
            Minecraft.getInstance().getTextureManager().release(textureId);
        }
    }
}
