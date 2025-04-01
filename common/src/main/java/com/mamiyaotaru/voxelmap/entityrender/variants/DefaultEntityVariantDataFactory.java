package com.mamiyaotaru.voxelmap.entityrender.variants;

import com.mamiyaotaru.voxelmap.entityrender.EntityVariantData;
import com.mamiyaotaru.voxelmap.entityrender.EntityVariantDataFactory;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

public class DefaultEntityVariantDataFactory implements EntityVariantDataFactory {
    private final EntityType<?> type;
    private final ResourceLocation secondaryTexture;

    public DefaultEntityVariantDataFactory(EntityType<?> type) {
        this(type, null);
    }

    public DefaultEntityVariantDataFactory(EntityType<?> type, ResourceLocation secondaryTexture) {
        this.type = type;
        this.secondaryTexture = secondaryTexture;
    }

    @Override
    public EntityType<?> getType() {
        return type;
    }

    @Override
    public <T extends LivingEntity, S extends LivingEntityRenderState> EntityVariantData createVariantData(T entity, LivingEntityRenderer<T, S, ?> renderer, S state, int size, boolean addBorder) {
        return new DefaultEntityVariantData(type, renderer.getTextureLocation(state), secondaryTexture, size, addBorder);
    }

    public static <T extends LivingEntity, S extends LivingEntityRenderState> EntityVariantData createSimpleVariantData(T entity, LivingEntityRenderer<T, S, ?> renderer, S state, int size, boolean addBorder) {
        return new DefaultEntityVariantData(entity.getType(), renderer.getTextureLocation(state), null, size, addBorder);
    }

}
