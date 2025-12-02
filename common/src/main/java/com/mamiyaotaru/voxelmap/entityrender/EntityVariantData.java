package com.mamiyaotaru.voxelmap.entityrender;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

public interface EntityVariantData {

    public EntityType<?> getType();

    public Identifier getPrimaryTexture();

    public Identifier getSecondaryTexture();
}
