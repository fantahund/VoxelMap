package com.mamiyaotaru.voxelmap.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.renderer.texture.AbstractTexture;

public class RegisterableGPUTexture extends AbstractTexture {
    private boolean refreshDepthTexture;
    private GpuTexture depthTexture;
    private GpuTextureView depthTextureView;

    public RegisterableGPUTexture(GpuTexture texture) {
        setTexture(texture);
    }

    public RegisterableGPUTexture(GpuTexture texture, GpuTextureView textureView) {
        setTexture(texture, textureView);
    }

    public int getWidth(int i) {
        if (this.texture != null) {
            return this.texture.getWidth(i);
        }
        return 0;
    }

    public int getHeight(int i) {
        if (this.texture != null) {
            return this.texture.getHeight(i);
        }
        return 0;
    }

    public void setTexture(GpuTexture texture) {
        RenderSystem.assertOnRenderThread();
        setTexture(texture, RenderSystem.getDevice().createTextureView(texture));
    }

    public void setTexture(GpuTexture texture, GpuTextureView textureView) {
        if (this.texture != null) {
            this.texture.close();
        }
        if (this.textureView != null) {
            this.textureView.close();
        }

        this.texture = texture;
        this.textureView = textureView;

        this.refreshDepthTexture = true;
    }

    private void setupDepthTexture() {
        RenderSystem.assertOnRenderThread();
        if (this.refreshDepthTexture) {
            this.refreshDepthTexture = false;

            if (this.depthTexture != null) {
                this.depthTexture.close();
            }
            if (this.depthTextureView != null) {
                this.depthTextureView.close();
            }

            this.depthTexture = RenderSystem.getDevice().createTexture(
                    this.texture.getLabel() + "-Depth",
                    this.texture.usage(),
                    TextureFormat.DEPTH32,
                    this.texture.getWidth(0),
                    this.texture.getHeight(0),
                    this.texture.getDepthOrLayers(),
                    this.texture.getMipLevels()
            );

            this.depthTextureView = RenderSystem.getDevice().createTextureView(this.depthTexture);
        }
    }

    public GpuTexture getDepthTexture() {
        setupDepthTexture();
        return this.depthTexture;
    }

    public GpuTextureView getDepthTextureView() {
        setupDepthTexture();
        return this.depthTextureView;
    }
}