package com.mamiyaotaru.voxelmap.mixins;

import net.minecraft.client.model.monster.dragon.EnderDragonModel;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EnderDragonRenderer.class)
public interface AccessorEnderDragonRenderer {
    @Accessor("model")
    EnderDragonModel getModel();

    @Accessor("DRAGON_LOCATION")
    static Identifier getTextureLocation() {
        throw new AssertionError();
    }
}
