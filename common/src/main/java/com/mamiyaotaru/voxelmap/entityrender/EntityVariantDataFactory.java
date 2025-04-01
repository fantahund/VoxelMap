package com.mamiyaotaru.voxelmap.entityrender;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

public interface EntityVariantDataFactory {

    EntityType<?> getType();

    <T extends LivingEntity, S extends LivingEntityRenderState> EntityVariantData createVariantData(T entity, LivingEntityRenderer<T, S, ?> renderer, S state, int size, boolean addBorder);

}