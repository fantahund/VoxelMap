package com.mamiyaotaru.voxelmap.mixins;

import net.minecraft.client.model.dragon.EnderDragonModel;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EnderDragonRenderer.class)
public interface AccessorEnderDragonRenderer {
    @Accessor("model")
    EnderDragonModel getModel();

    @Accessor("DRAGON_LOCATION")
    static ResourceLocation getTextureLocation() {
        throw new AssertionError();
    }
}
