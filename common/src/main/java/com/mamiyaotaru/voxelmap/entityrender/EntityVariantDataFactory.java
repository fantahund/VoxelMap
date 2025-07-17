package com.mamiyaotaru.voxelmap.entityrender;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public interface EntityVariantDataFactory {

    EntityType<?> getType();

    EntityVariantData createVariantData(Entity entity, @SuppressWarnings("rawtypes") EntityRenderer renderer, EntityRenderState state, int size, boolean addBorder);

}