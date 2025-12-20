package com.mamiyaotaru.voxelmap.entityrender;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

public interface EntityVariantData {

    public EntityType<?> getType();

    public ResourceLocation getPrimaryTexture();

    public ResourceLocation getSecondaryTexture();
}
